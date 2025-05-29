package com.groom.marky.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OdysseyService {

    @Value("${ODYSSEY_API_KEY}")
    private String ODYSSEY_API_KEY;

    private final RestTemplate restTemplate = new RestTemplate();

    private String sendGetRequest(String url, Map<String, String> queryParams) {
        StringBuilder finalUrl = new StringBuilder(url + "?");

        //쿼리 파라미터 붙이기
        queryParams.forEach((key, value) -> finalUrl.append(key).append("=").append(value).append("&"));
        finalUrl.setLength(finalUrl.length() - 1); // 마지막 & 제거

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    finalUrl.toString(), HttpMethod.GET, entity, String.class
            );
            return response.getBody();
        } catch (Exception e) {
            return "오류 발생: " + e.getMessage();
        }
    }

    public String callSubwayPathApi(String SID, String EID) {
        return sendGetRequest("https://api.odsay.com/v1/api/subwayPath", Map.of(
                "apiKey", ODYSSEY_API_KEY,
                "CID", String.valueOf(1000),
                "SID", SID,
                "EID", EID,
                "Sopt", String.valueOf(2) // 1. 최단거리 (1),  2. 최소환승 (2)
        ));
    }
}