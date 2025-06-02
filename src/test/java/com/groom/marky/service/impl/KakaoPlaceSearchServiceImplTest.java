package com.groom.marky.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest
@Import(KakaoPlaceSearchServiceImpl.class)
class KakaoPlaceSearchServiceImplTest {

	@Autowired
	private KakaoPlaceSearchServiceImpl kakaoPlaceSearchService;

	/**
	 * 무엇을 테스트하고싶은지?
	 * 해당 메서드 내부에서 외부 API 를 호출한다.
	 * 정상인 상황과 예외 반환 상황을 테스트한다.
	 *
	 * 해야 할 일은, 외부 API가 정상으로 왔을 때, 원하는 응답의 형태로 오는지, 그리고 예외 발생 시 예외 처리가 가능한지.
	 */

	/**
	 * 조회 관련 테스트 그룹
	 */
	@Nested
	@DisplayName("API 호출 관련 테스트")
	class CallApiTests {

		@Autowired
		private RestTemplate restTemplate;

		private static final String KEYWORD_SEARCH_API_URI = "https://dapi.kakao.com/v2/local/search/keyword.json";
		private static final String ACCURACY_SORT = "accuracy";

		private MockRestServiceServer mockServer;

		@BeforeEach
		void setUp() {
			mockServer = MockRestServiceServer.createServer(restTemplate);
		}

		@AfterEach
		void clear() {
			mockServer.reset();
		}

		/**
		 * kakaoPlaceSearchService.search(keyword)가 응답의 documents[0].x/y를 정확히 파싱해서 Map<String, Double>에 넣는지 검증.
		 */
		@Test
		@DisplayName("카카오 키워드 서치 API 호출 - 성공 케이스")
		void kakaoApiKeywordSearchTest() {

			// Given : RestTemplate Response Body 생성
			String keyword = "서울역";
			String jsonResponse = """
				{
				  "documents": [
				    {
				      "address_name": "서울 중구 봉래동2가 122-11",
				      "category_group_code": "",
				      "category_group_name": "",
				      "category_name": "교통,수송 > 기차,철도 > 기차역 > KTX정차역",
				      "distance": "",
				      "id": "9113903",
				      "phone": "1544-7788",
				      "place_name": "서울역",
				      "place_url": "http://place.map.kakao.com/9113903",
				      "road_address_name": "서울 중구 한강대로 405",
				      "x": "126.97070335253385",
				      "y": "37.55406888733184"
				    }
				  ],
				  "meta": {
				    "is_end": false,
				    "pageable_count": 45,
				    "same_name": {
				      "keyword": "서울역",
				      "region": [],
				      "selected_region": ""
				    },
				    "total_count": 19574
				  }
				}
				""";

			URI uri = UriComponentsBuilder.fromUriString(KEYWORD_SEARCH_API_URI)
				.queryParam("page", 1)
				.queryParam("size", 1)
				.queryParam("query", keyword)
				.queryParam("sort", ACCURACY_SORT)
				.encode(StandardCharsets.UTF_8)
				.build().toUri();

			// mocking kakao api response
			mockServer.expect(requestTo(uri))
				.andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

			// When & Then
			String lat = "lat";
			String lon = "lon";
			Double valueX = 126.97070335253385;
			Double valueY = 37.55406888733184;

			Map<String, Double> response = kakaoPlaceSearchService.search(keyword);

			assertThat(response)
				.containsKey(lon)
				.containsKey(lat)
				.containsEntry(lon, valueX)
				.containsEntry(lat, valueY);
		}

		@DisplayName("카카오 키워드 서치 API 호출 - 응답 데이터가 존재하지 않는 케이스")
		@Test
		void test() {
			// given

			// when

			// then
		}

	}

}
