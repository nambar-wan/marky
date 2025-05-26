package com.groom.marky.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.marky.common.constant.CsvType;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SeoulInfoApiServiceImpl implements SeoulInfoApiService {
    private final int BATCH_SIZE = 1000;
    @Value("${SEOUL_DATASET_API_KEY}")
    private final String apiKey;
    private final HttpEntity<Void> httpEntity;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final VectorStore vectorStore;

    private static final String SEOUL_API = "http://openapi.seoul.go.kr:8088";
    private static final String TYPE = "json";

    @Autowired
    public SeoulInfoApiServiceImpl(RestTemplate restTemplate,
                                   ObjectMapper objectMapper,
                                   @Value("${SEOUL_DATASET_API_KEY}") String apiKey,
                                   VectorStore vectorStore) {

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
                        .pathSegment(apiKey, TYPE, serviceType, String.valueOf(start), String.valueOf(start + BATCH_SIZE - 1))
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

            String name = record.get("FCLTY_NM");
            String category = record.get("MLSFC_NM");
            String tel = record.get("TEL_NO");
            String latitude = record.get("FCLTY_LA");
            String longitude = record.get("FCLTY_LO");
            String fee = record.get("VIEWNG_PRICE");
            String address = record.get("SIGNGU_NM");

            String content = String.format(
                "%s 미술관은 %s 분류에 속하며, 위치는 %s (%s, %s)입니다. 연락처는 %s, 관람료는 %s입니다.",
                name, category, address, latitude, longitude, tel, fee
            );

            Map<String, Object> metadata = Map.of(
                "name", name,
                "category", category,
                "tel", tel,
                "latitude", latitude,
                "longitude", longitude,
                "fee", fee,
                "address", address,
                "type", "gallery"
            );

            documents.add(new Document(content, metadata));
            result.put(name, content);
        }

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
            String routeSummary = record.get("COURS_DC");
            String distanceCategory = record.get("COURS_LT_CN");
            String distanceKm = record.get("COURS_DETAIL_LT_CN");
            String description = record.get("ADIT_DC");
            String duration = record.get("COURS_TIME_CN");
            String waterSupply = record.get("OPTN_DC");
            String facility = record.get("CVNTL_NM");
            String store = record.get("TOILET_DC");
            String addr = record.get("LNM_ADDR");
            String lat = record.get("COURS_SPOT_LA");
            String lon = record.get("COURS_SPOT_LO");

            String content = String.format(
                "산책코스 '%s' 입니다. 설명: %s 거리: %s (%s) 소요 시간: %s 주소: %s 주요 지점: (%s, %s) 부가정보: 식수=%s, 편의시설=%s, 매점=%s",
                trailName, description, distanceKm, distanceCategory,
                duration, addr, lat, lon, waterSupply, facility, store
            );

            Map<String, Object> metadata = Map.of(
                    "trailName",trailName,
                    "summary", String.format("%s - %s", routeSummary, description),
                    "distanceCategory", distanceCategory,
                    "distanceKm", distanceKm,
                    "duration", duration,
                    "waterSupply", waterSupply,
                    "facility", facility,
                    "store", store,
                    "addr", addr,
                    "location", String.format("(%s, %s)", lat, lon)
            );

            documents.add(new Document(content, metadata));
            result.put(trailName, content);
        }

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
            String branchName = record.get("BHF_NM");
            String name = poiName + (branchName != null && !branchName.isBlank() ? " " + branchName : "");

            String district = record.get("SIGNGU_NM");
            String neighborhood = record.get("LEGALDONG_NM");
            String ri = record.get("LI_NM");
            String number = record.get("LNBR_NO");
            String address = String.format("%s %s %s %s", district, neighborhood, ri, number).replaceAll("\\s+", " ").trim();

            String longitude = record.get("LC_LO");
            String latitude = record.get("LC_LA");

            String category = record.get("CL_NM");
            String roadName = record.get("RDNMADR_NM");
            String lastChanged = record.get("LAST_CHG_DE");
            String origin = record.get("ORIGIN_NM");

            String content = String.format(
                "%s 영화관(%s). 위치는 %s, 도로명 주소는 %s입니다. 좌표는 (%s, %s)이며, 마지막 변경일은 %s, 출처는 %s입니다.",
                name, category, address, roadName, latitude, longitude, lastChanged, origin
            );

            Map<String, Object> metadata = Map.of(
                "name", name,
                "category", category,
                "address", address,
                "roadName", roadName,
                "latitude", latitude,
                "longitude", longitude,
                "lastChanged", lastChanged,
                "origin", origin,
                "type", "theater"
            );

            documents.add(new Document(content, metadata));
            result.put(name, content);
        }

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
            String adminPhone = row.path("P_ADMINTEL").asText();
            String addr = row.path("P_ADDR").asText();
            String mainEquip = row.path("MAIN_EQUIP").asText();

            String content = String.format(
                    "%s 공원 안내: %s 이용안내: %s 방문경로: %s 참고사항: %s 연락처: %s 위치: %s 주요시설: %s",
                    parkName, parkInfo, guide, visitRoad, useReference, adminPhone, addr, mainEquip
            );

            Map<String, Object> metadata = Map.of(
                    "parkName", parkName,
                    "guide", guide,
                    "visitRoad", visitRoad,
                    "useReference", useReference,
                    "adminPhone", adminPhone,
                    "address", addr,
                    "mainEquip", mainEquip
            );

            documents.add(new Document(content, metadata));
            result.put(parkName, content);
        }

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
            String registerDate = row.path("RGSTDATE").asText();
            String content = String.format(
                    "%s에서 열리는 '%s' 문화행사. 장소는 %s, 날짜는 %s, 관람료는 %s입니다.",
                    location, title, place, date, fee
            );

            Map<String, Object> metadata = Map.of(
                    "title", title,
                    "location", location,
                    "place", place,
                    "date", date,
                    "fee", fee,
                    "registerDate", registerDate
            );

            documents.add(new Document(content, metadata));
            result.put(title, content);
        }

        vectorStore.add(documents);
    }
}
