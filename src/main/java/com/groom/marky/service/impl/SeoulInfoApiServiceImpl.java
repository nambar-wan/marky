package com.groom.marky.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.marky.common.constant.CsvType;
import com.groom.marky.common.constant.GooglePlaceType;
import com.groom.marky.service.SeoulInfoApiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.groom.marky.common.constant.MetadataKeys.*;

@Slf4j
@Service
public class SeoulInfoApiServiceImpl implements SeoulInfoApiService {
    private final GooglePlaceSearchServiceImpl googlePlaceSearchService;
    private final RedisService redisService;
    private final int BATCH_SIZE = 1000;
    @Value("${SEOUL_DATASET_API_KEY}")
    private final String apiKey;
    private final HttpEntity<Void> httpEntity;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final VectorStore vectorStore;

    private static final String SEOUL_API = "http://openapi.seoul.go.kr:8088";
    private static final String JSON = "json";

    @Autowired
    public SeoulInfoApiServiceImpl(GooglePlaceSearchServiceImpl googlePlaceSearchService, RedisService redisService, RestTemplate restTemplate,
                                   ObjectMapper objectMapper,
                                   @Value("${SEOUL_DATASET_API_KEY}") String apiKey,
                                   VectorStore vectorStore) {
        this.googlePlaceSearchService = googlePlaceSearchService;
        this.redisService = redisService;

        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.vectorStore = vectorStore;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        this.httpEntity = new HttpEntity<>(headers);
    }

    @Override
    public Map<String, String> apiCall(String serviceType) {

        int start = 1;
        int totalCount = Integer.MAX_VALUE;
        HashMap<String, String> result = new HashMap<>();

        try {
            while (start <= totalCount) {

                URI uri = UriComponentsBuilder.fromUriString(SEOUL_API)
                        .pathSegment(apiKey, JSON, serviceType, String.valueOf(start), String.valueOf(start + BATCH_SIZE - 1))
                        .build(true)
                        .toUri();

                // 요청 -> 응답 ( 단순 문자열 )
                ResponseEntity<String> response =
                        restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);

                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path(serviceType);
                JsonNode rows = data.path("row");

                if (totalCount == Integer.MAX_VALUE) {
                    totalCount = data.path("list_total_count").asInt();
                }

                switch (serviceType) {
                    case "culturalEventInfo":
                        festivalInfo(rows, result);
                        break;

                    case "SearchParkInfoService":
                        gardenInfo(rows, result);
                        break;

                    default:
                        log.warn("지원하지 않는 SERVICE_TYPE입니다: {}", serviceType);
                        break;
                }
                start += BATCH_SIZE;
            }
        } catch (JsonProcessingException e) {
            log.info(e.getMessage());
        }catch (Exception e) {
            log.warn("서울 정보 API 호출 실패: {}", e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, String> csvImport(MultipartFile multipartFile, CsvType type) {
        try (Reader reader = new InputStreamReader(multipartFile.getInputStream(), StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreEmptyLines()
                .withTrim()
                .parse(reader);

            switch (type) {
                case GALLERY:
                    return parseGalleryCsv(records);
                case TRAIL:
                    return parseTrailCsv(records);
                case THEATER:
                    return parseTheaterCsv(records);
                default:
                    log.warn("지원하지 않는 CSV 타입입니다: {}", type);
                    return Map.of();
            }

        } catch (IOException e) {
            log.error("CSV 파일 처리 실패", e);
            return Map.of();
        }
    }

    private Map<String, String> parseGalleryCsv(Iterable<CSVRecord> records) {
        Map<String, String> result = new HashMap<>();
        List<Document> documents = new ArrayList<>();

        for (CSVRecord record : records) {
            String region = record.get("CTPRVN_NM");
            if (!region.contains("서울")) continue;

            String galleryName = record.get("FCLTY_NM");
            if(galleryName == null || galleryName.isBlank()){
                continue;
            }
            String tel = record.get("TEL_NO");
            String latitude = record.get("FCLTY_LA");
            String longitude = record.get("FCLTY_LO");
            String fee = record.get("VIEWNG_PRICE");
            String address = record.get("SIGNGU_NM");

            String content = String.format(
                "%s 미술관 위치는 %s (%s, %s)입니다. 연락처는 %s, 관람료는 %s입니다.",
                    galleryName, address, latitude, longitude, tel, fee
            );

            if (latitude == null || latitude.isBlank() || longitude == null || longitude.isBlank()) {
                log.warn("좌표 정보 누락 - parkName: {}, lat: '{}', lon: '{}'", galleryName, latitude, longitude);
                continue;
            }
            String googlePlaceId = googlePlaceSearchService.searchPlaceId(galleryName);
            if(googlePlaceId == null || googlePlaceId.isBlank()){
                continue;
            }
            String id = UUID.nameUUIDFromBytes(googlePlaceId.getBytes(StandardCharsets.UTF_8)).toString();

            Map<String, Object> metadata = Map.of(
                DISPLAYNAME, galleryName,
                "tel", tel,
                LAT, latitude,
                LON, longitude,
                "fee", fee,
                FORMATTEDADDRESS, address,
                TYPE, "activity",
                ACTIVITY_TYPE, "미술관",
                GOOGLEPLACEID, googlePlaceId
            );
            documents.add(new Document(id, content, metadata));
            result.put(galleryName, content);
        }

        redisService.setSeoulPlacesLocation(GooglePlaceType.ACTIVITY,documents);
        vectorStore.add(documents);
        return result;
    }

    private Map<String, String> parseTrailCsv(Iterable<CSVRecord> records) {
        Map<String, String> result = new HashMap<>();
        List<Document> documents = new ArrayList<>();

        for (CSVRecord record : records) {
            String trailRegion = record.get("SIGNGU_NM");
            if (!trailRegion.contains("서울")) continue;

            String trailName = record.get("WLK_COURS_NM");
            if(trailName == null || trailName.isBlank()){
                continue;
            }
            String routeSummary = record.get("COURS_DC");
            String distanceKm = record.get("COURS_DETAIL_LT_CN");
            String description = record.get("ADIT_DC");
            String duration = record.get("COURS_TIME_CN");
            String addr = record.get("LNM_ADDR");
            String latitude = record.get("COURS_SPOT_LA");
            String longitude = record.get("COURS_SPOT_LO");

            String content = String.format(
                "산책코스 '%s' 입니다. 설명: %s 거리: %s 소요 시간: %s 주소: %s : (%s, %s)",
                trailName, description, distanceKm, duration, addr, latitude, longitude);
            if (latitude == null || latitude.isBlank() || longitude == null || longitude.isBlank()) {
                log.warn("좌표 정보 누락 - parkName: {}, lat: '{}', lon: '{}'", trailName, latitude, longitude);
                continue;
            }
            String googlePlaceId = googlePlaceSearchService.searchPlaceId(trailName);
            if(googlePlaceId == null || googlePlaceId.isBlank()){
                continue;
            }
            String id = UUID.nameUUIDFromBytes(googlePlaceId.getBytes(StandardCharsets.UTF_8)).toString();

            Map<String, Object> metadata = Map.of(
                GOOGLEPLACEID, googlePlaceId,
                DISPLAYNAME,trailName,
                LAT, latitude,
                LON, latitude,
                FORMATTEDADDRESS, addr,
                TYPE, "activity",
                ACTIVITY_TYPE, "산책길",
                "summary", String.format("%s - %s", routeSummary, description),
                "distanceKm", distanceKm,
                "duration", duration
            );

            documents.add(new Document(id, content, metadata));
            result.put(trailName, content);
        }
        redisService.setSeoulPlacesLocation(GooglePlaceType.ACTIVITY,documents);
        vectorStore.add(documents);
        return result;
    }

    private Map<String, String> parseTheaterCsv(Iterable<CSVRecord> records) {
        Map<String, String> result = new HashMap<>();
        List<Document> documents = new ArrayList<>();

        for (CSVRecord record : records) {
            String theaterRegion = record.get("CTPRVN_NM");
            if (!theaterRegion.contains("서울")) continue;

            String poiName = record.get("POI_NM");
            if(poiName == null || poiName.isBlank()){
                continue;
            }
            String branchName = record.get("BHF_NM");
            String name = poiName + (branchName != null && !branchName.isBlank() ? " " + branchName : "");

            String district = record.get("SIGNGU_NM");
            String neighborhood = record.get("LEGALDONG_NM");
            String ri = record.get("LI_NM");
            String number = record.get("LNBR_NO");
            String address = String.format("%s %s %s %s", district, neighborhood, ri, number).replaceAll("\\s+", " ").trim();

            String longitude = record.get("LC_LO");
            String latitude = record.get("LC_LA");

            String content = String.format(
                "%s 영화관 위치는 %s 좌표는 (%s, %s)입니다",
                name, address, latitude, longitude
            );

            if (latitude == null || latitude.isBlank() || longitude == null || longitude.isBlank()) {
                log.warn("좌표 정보 누락 - parkName: {}, lat: '{}', lon: '{}'", poiName, latitude, longitude);
                continue;
            }
            String googlePlaceId = googlePlaceSearchService.searchPlaceId(name);
            if(googlePlaceId == null || googlePlaceId.isBlank()){
                continue;
            }
            String id = UUID.nameUUIDFromBytes(googlePlaceId.getBytes(StandardCharsets.UTF_8)).toString();

            Map<String, Object> metadata = Map.of(
                GOOGLEPLACEID, googlePlaceId,
                TYPE, "activity",
                ACTIVITY_TYPE, "영화관",
                DISPLAYNAME, name,
                FORMATTEDADDRESS, address,
                LAT, latitude,
                LON, longitude
            );

            documents.add(new Document(id, content, metadata));
            result.put(name, content);
        }

        redisService.setSeoulPlacesLocation(GooglePlaceType.ACTIVITY,documents);
        vectorStore.add(documents);
        return result;
    }

    private void gardenInfo(JsonNode rows, HashMap<String, String> result) {
        List<Document> documents = new ArrayList<>();

        for (JsonNode row : rows) {
            String parkName = row.path("P_PARK").asText();
            String parkInfo = row.path("P_LIST_CONTENT").asText();
            String guide = row.path("GUIDANCE").asText();
            String visitRoad = row.path("VISIT_ROAD").asText();
            String useReference = row.path("USE_REFER").asText();
            String addr = row.path("P_ADDR").asText();
            String longitude = row.path("LONGITUDE").asText();
            String latitude = row.path("LATITUDE").asText();

            String content = String.format(
                    "%s 공원 안내: %s 이용안내: %s 방문경로: %s 참고사항: %s 위치: %s",
                    parkName, parkInfo, guide, visitRoad, useReference, addr
            );
            if(parkName == null || parkName.isBlank()){
                continue;
            }
            if (latitude == null || latitude.isBlank() || longitude == null || longitude.isBlank()) {
                log.warn("좌표 정보 누락 - parkName: {}, lat: '{}', lon: '{}'", parkName, latitude, longitude);
                continue;
            }
            String googlePlaceId = googlePlaceSearchService.searchPlaceId(parkName);
            String id = UUID.nameUUIDFromBytes(googlePlaceId.getBytes(StandardCharsets.UTF_8)).toString();

            Map<String, Object> metadata = Map.of(
                    GOOGLEPLACEID, googlePlaceId,
                    TYPE, "activity",
                    ACTIVITY_TYPE, "공원",
                    DISPLAYNAME, parkName,
                    LAT, latitude,
                    LON, longitude,
                    "guide", guide,
                    "visitRoad", visitRoad,
                    "useReference", useReference,
                    FORMATTEDADDRESS, addr
            );

            documents.add(new Document(id, content, metadata));
            result.put(parkName, content);
        }

        redisService.setSeoulPlacesLocation(GooglePlaceType.ACTIVITY,documents);
        vectorStore.add(documents);
    }

    private void festivalInfo(JsonNode rows, HashMap<String, String> result) {
        List<Document> documents = new ArrayList<>();

        for (JsonNode row : rows) {
            String location = row.path("GUNAME").asText();
            String title = row.path("TITLE").asText();
            String place = row.path("PLACE").asText();
            String date = row.path("DATE").asText();
            String fee = row.path("USE_FEE").asText();
            String latitude = row.path("LAT").asText();
            String longitude = row.path("LOT").asText();

            String content = String.format(
                    "%s에서 열리는 '%s' 문화행사. 장소는 %s, 날짜는 %s, 관람료는 %s입니다.",
                    location, title, place, date, fee
            );

            Map<String, Object> metadata = Map.of(
                    TYPE, "activity",
                    ACTIVITY_TYPE, "행사",
                    DISPLAYNAME, title,
                    LAT, latitude,
                    LON, longitude,
                    FORMATTEDADDRESS, location,
                    "place", place,
                    "date", date,
                    "fee", fee
            );

            documents.add(new Document(content, metadata));
            result.put(title, content);
        }

        redisService.setSeoulPlacesLocation(GooglePlaceType.ACTIVITY,documents);
        vectorStore.add(documents);
    }
}
