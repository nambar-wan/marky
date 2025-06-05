package com.groom.marky.service.impl;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.groom.marky.domain.response.GooglePlacesApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.marky.domain.request.Rectangle;
import com.groom.marky.common.constant.KakaoMapCategoryGroupCode;
import com.groom.marky.service.KakaoPlaceSearchService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class KakaoPlaceSearchServiceImpl implements KakaoPlaceSearchService {

	// 카카오를 통한 장소 조회
	// api key, url, code 등등.. 호출 시 필요한 restTemplate, Uri 등등 다 가져다놓기

	@Value("${KAKAO_REST_API_KEY}")
	private final String apiKey; // @Value로 주입하는 필드는 static 일 수 없음.
	private final HttpEntity<Void> httpEntity;
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final GooglePlaceSearchServiceImpl googlePlaceSearchService;


	private static final String KEYWORD_SEARCH_API_URI = "https://dapi.kakao.com/v2/local/search/keyword.json";
	private static final String CATEGORY_SEARCH_API_URI = "https://dapi.kakao.com/v2/local/search/category.json";
	private static final String ACCURACY_SORT = "accuracy";
	private static final String DISTANCE_SORT = "distance";

	@Autowired
	public KakaoPlaceSearchServiceImpl(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${KAKAO_REST_API_KEY}") String apiKey,
			GooglePlaceSearchServiceImpl googlePlaceSearchService // 이렇게 주입하면 되는군
	) {
		this.restTemplate = restTemplate;
		this.objectMapper = objectMapper;
		this.apiKey = apiKey;
		this.googlePlaceSearchService = googlePlaceSearchService;

		// 한 번만 생성해서 재사용 가능한 final 필드로 초기화
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey);

		// 제네릭 타입 <Void> 을 지정해서 raw‐use 경고 해소
		this.httpEntity = new HttpEntity<>(headers);
	}

	@Override
	public Map<String, String> search(String rect, KakaoMapCategoryGroupCode code) {

		int page = 1;
		boolean isEnd = false;

		HashMap<String, String> result = new HashMap<>();

		try {
			while (!isEnd) {

				URI uri = buildCategoryUri(page, rect, code);

				// 요청 -> 응답 ( 단순 문자열 )
				ResponseEntity<String> response =
					restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);

				JsonNode root = objectMapper.readTree(response.getBody());
				JsonNode documents = root.path("documents");
				JsonNode meta = root.path("meta");

				// 1. 저장
				for (JsonNode document : documents) {
					String id = document.path("id").asText();
					String placeName = document.path("place_name").asText();
					String addressName = document.path("address_name").asText();
					String province = addressName.split(" ")[0];

					if (!province.startsWith("서울") || result.containsKey(id)) {
						continue;
					}
					result.put(id, placeName);
				}

				// 2. 다음 페이지 여부 확인
				isEnd = meta.path("is_end").asBoolean(true);
				page++;
			}
		} catch (JsonProcessingException e) {
			log.info(e.getMessage());
		}
		return result;
	}

	@Override
	public Map<String, String> searchAll(List<Rectangle> boxes, KakaoMapCategoryGroupCode code) {

		HashMap<String, String> result = new HashMap<>();
		ArrayDeque<Rectangle> queue = new ArrayDeque<>(boxes);

		// 개별 박스 큐
		while (!queue.isEmpty()) {

			Rectangle box = queue.poll();
			String rect = box.toString();

			int total = getTotalCount(rect, code);

			if (total == 0) {
				continue;
			}

			if (total > 60) {
				// 분리
				queue.addAll(box.splitGrid());
				continue;
			}

			Map<String, String> searchResult = search(rect, code);

			result.putAll(searchResult);
		}
		return result;
	}

	@Override
	public Map<String, Double> searchLocation(String keyword, String category_code) {
		HashMap<String, Double> result = new HashMap<>();
		JsonNode finalNode = null;
		try {
			// 1. 첫 검색
			if(category_code == null || category_code.isBlank() || "null".equals(category_code)) {
				log.info("keyWord Search category : {}",category_code);
				URI uri = buildKeywordUri(keyword);
				//Todo 메서드 추출 가능
				ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);
				JsonNode root = objectMapper.readTree(response.getBody());
				JsonNode firstNode = root.path("documents");

				if (!firstNode.isArray() || firstNode.isEmpty()) return result;

				finalNode = firstNode.get(0);

			}else {
				try {
					log.info("Category Search");

					KakaoMapCategoryGroupCode categoryCode = KakaoMapCategoryGroupCode.valueOf(category_code.toUpperCase());
					URI uri = buildLocationChangeKeywordUri(keyword, categoryCode);
					ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);
					JsonNode documents = objectMapper.readTree(response.getBody()).path("documents");

					if (documents.isArray() && !documents.isEmpty() && !documents.get(0).isNull()) {
						finalNode = documents.get(0);
						log.info("카테고리 검색 성공 → 좌표 : {}, {}", finalNode.path("x"), finalNode.path("y"));
					}
				} catch (IllegalArgumentException iae) {
					log.warn("유효하지 않은 category: {}", category_code);
					return result;
				}
			}

			if (finalNode != null) {
				result.put("lat", finalNode.path("y").asDouble());
				result.put("lon", finalNode.path("x").asDouble());
			}

		} catch (Exception e) {
			log.warn("[KakaoPlaceSearchServiceImpl] search 예외 발생: keyword={}, message={}", keyword, e.getMessage());
		}

		return result;
	}


	/**
	 *
	 * @param
	 * keyword 검색 키워드
	 *
	 * @return
	 * Map<String, Double> lat : 위도값, lon : 경도값
	 */
	@Override
	public Map<String, Double> search(String keyword) {
		HashMap<String, Double> result = new HashMap<>();

		try {
			URI uri = buildKeywordUri(keyword);

			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);

			String body = response.getBody();
			JsonNode root = objectMapper.readTree(body);
			JsonNode documents = root.path("documents");
			JsonNode firstNode = documents.get(0);

			result.put("lat", Double.parseDouble(firstNode.get("y").textValue()));
			result.put("lon", Double.parseDouble(firstNode.get("x").textValue()));

		} catch (Exception e) {
			log.info("[KakaoPlaceSearchServiceImpl] search 예외 발생  keyword : {}, message : {} ", keyword, e.getMessage());
		}
		return result;

	}

	@Override
	public int getTotalCount(String rect, KakaoMapCategoryGroupCode code) {

		int result = 0;

		try {
			URI uri = buildCategoryCountUri(rect, code);
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);

			JsonNode meta = objectMapper.readTree(response.getBody()).path("meta");
			result = meta.path("total_count").asInt();
		} catch (JsonProcessingException e) {
			log.info("[KakaoPlaceSearchServiceImpl] searchTotalParkingLotCountByRect 예외 발생 : {}", e.getMessage());
		}
		return result;
	}

	@Override
	public Set<Rectangle> getRects(List<Rectangle> rects, KakaoMapCategoryGroupCode code) {

		Set<Rectangle> result = new HashSet<>();
		ArrayDeque<Rectangle> queue = new ArrayDeque<>(rects);

		// 개별 박스 큐
		while (!queue.isEmpty()) {
			Rectangle rect = queue.poll();
			int total = getTotalCount(rect.toString(), code);

			if (total == 0) {
				continue;
			}

			if (total > 60) {
				// 분리
				queue.addAll(rect.splitGrid());
				continue;
			}

			log.info("rect : {}, total : {}", rect, total);
			result.add(rect);
		}
		return result;
	}

	@Override
	public GooglePlacesApiResponse getRects(List<Rectangle> rects, String keyword) {
		Set<Rectangle> kakaoResult = new HashSet<>();
		ArrayDeque<Rectangle> queue = new ArrayDeque<>(rects);

		// 개별 박스 큐
		while (!queue.isEmpty()) {
			Rectangle rect = queue.poll();
			int total = getTotalCount(rect.toString(), keyword);
			log.info("rect : {}, total : {}", rect, total);

			if (total == 0) {
				continue;
			}

			if (total > 60) {
				// 분리
				queue.addAll(rect.splitGrid());
				continue;
			}

			kakaoResult.add(rect);
		}
		// Use GooglePlacesApiResponse.Place instead of raw String
		return googlePlaceSearchService.search(keyword, kakaoResult);
	}

	@Override
	public Map<Rectangle, Integer> getRectsMap(List<Rectangle> rects, KakaoMapCategoryGroupCode code){
		Map<Rectangle, Integer> result = new HashMap<>();
		ArrayDeque<Rectangle> queue = new ArrayDeque<>(rects);

		// 개별 박스 큐
		while (!queue.isEmpty()) {
			Rectangle rect = queue.poll();
			int total = getTotalCount(rect.toString(), code);

			if (total == 0) {
				continue;
			}

			if (total > 60) {
				// 분리
				queue.addAll(rect.splitGrid());
				continue;
			}

			log.info("rect : {}, total : {}", rect, total);
			result.put(rect, total);
		}
		return result;
	}

	@Override
	public int getTotalCount(String rect, String keyword) {

		int result = 0;

		try {
			URI uri = buildKeywordUri(rect, keyword);
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);

			JsonNode meta = objectMapper.readTree(response.getBody()).path("meta");
			result = meta.path("total_count").asInt();
			log.info("total_count : {}", result);
		} catch (JsonProcessingException e) {
			log.info("searchTotalParkingLotCountByRect 예외 발생 : {}", e.getMessage());
		}
		return result;
	}

	private static URI buildKeywordUri(String keyword) {
		return UriComponentsBuilder.fromUriString(KEYWORD_SEARCH_API_URI)
			.queryParam("page", 1)
			.queryParam("size", 1)
			.queryParam("query", keyword)
			.queryParam("sort", ACCURACY_SORT)
			.encode(StandardCharsets.UTF_8)
			.build().toUri();
	}

	private static URI buildKeywordUri(String rect, String keyword) {
		return UriComponentsBuilder.fromUriString(KEYWORD_SEARCH_API_URI)
			.queryParam("page", 1)
			.queryParam("size", 1)
			.queryParam("rect", rect)
			.queryParam("query", keyword)
			.encode(StandardCharsets.UTF_8)
			.build().toUri();
	}
	private static URI buildLocationChangeKeywordUri(String keyword, KakaoMapCategoryGroupCode categoryGroupCode) {
		return UriComponentsBuilder.fromUriString(KEYWORD_SEARCH_API_URI)
				.queryParam("query", keyword)
				.queryParam("category_group_code", categoryGroupCode)
				.encode(StandardCharsets.UTF_8)
				.build().toUri();
	}

	private static URI buildCategoryCountUri(String rect, KakaoMapCategoryGroupCode categoryGroupCode) {
		return UriComponentsBuilder.fromUriString(CATEGORY_SEARCH_API_URI)
			//.queryParam("category_group_code", category)
			.queryParam("page", 1)
			.queryParam("size", 1)
			.queryParam("rect", rect)
			.queryParam("sort", ACCURACY_SORT)
			.queryParam("category_group_code", categoryGroupCode)
			.encode(StandardCharsets.UTF_8)
			.build().toUri();
	}

	private static URI buildCategoryUri(int page, String rect, KakaoMapCategoryGroupCode categoryGroupCode) {
		return UriComponentsBuilder.fromUriString(CATEGORY_SEARCH_API_URI)
			.queryParam("page", page)
			.queryParam("size", 15)
			.queryParam("sort", ACCURACY_SORT)
			.queryParam("rect", rect)
			.queryParam("category_group_code", categoryGroupCode)
			.encode(StandardCharsets.UTF_8)
			.build().toUri();
	}

}





