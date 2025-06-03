package com.groom.marky.service.impl;

import java.net.URI;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.groom.marky.common.exception.OAuthProcessingException;
import com.groom.marky.domain.response.GoogleTokenResponse;
import com.groom.marky.domain.response.GoogleUserInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GoogleOAuthService {

	@Value("${google.oauth.client-id}")
	private String clientId;

	@Value("${google.oauth.client-secret}")
	private String clientSecret;

	@Value("${google.oauth.redirect-uri}")
	private String redirectUri;

	private static final String GOOGLE_AUTH_BASE_URI = "https://accounts.google.com/o/oauth2/v2/auth";
	private static final String AUTHORIZATION_URI = "https://accounts.google.com/o/oauth2/v2/auth";
	private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
	private static final String USER_INFO_URI = "https://www.googleapis.com/oauth2/v3/userinfo";

	private final RestTemplate restTemplate;

	@Autowired
	public GoogleOAuthService(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public String getAccessToken(String code) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		// MediaType.APPLICATION_FORM_URLENCODED 타입을 LinkedMultiValueMap 다루면 편하다고 합니다.
		// LinkedMultiValueMap 사용 시 Content-Type: application/x-www-form-urlencoded일 때 내부적으로 key=value&key2=value2 포맷으로 변환해줌
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("code", code);
		body.add("client_id", clientId);
		body.add("client_secret", clientSecret);
		body.add("redirect_uri", redirectUri);
		body.add("grant_type", "authorization_code");

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

		ResponseEntity<GoogleTokenResponse> response = restTemplate.exchange(
			TOKEN_URI, // https://oauth2.googleapis.com/token
			HttpMethod.POST,
			request,
			GoogleTokenResponse.class
		);

		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new OAuthProcessingException("구글 토큰 요청 실패: " + response.getStatusCode());
		}

		GoogleTokenResponse tokenResponse = response.getBody();
		if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
			throw new OAuthProcessingException("구글 토큰 응답이 null이거나 access_token 없음");
		}

		log.info("response : {}", tokenResponse);
		return tokenResponse.getAccessToken();
	}

	public String getLoginUri() {
		URI baseUri = URI.create(GOOGLE_AUTH_BASE_URI); // https://accounts.google.com/o/oauth2/v2/auth

		return UriComponentsBuilder.fromUri(baseUri)
			.queryParam("client_id", clientId)
			.queryParam("redirect_uri", redirectUri)
			.queryParam("response_type", "code")
			.queryParam("scope", "profile email")
			.queryParam("access_type", "offline")
			.build()
			.toUriString();
	}

	public GoogleUserInfo getUserInfo(String accessToken) {

		// 헤더에 토큰 담아서 요청
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);

		HttpEntity<Object> httpEntity = new HttpEntity<>(headers);

		ResponseEntity<GoogleUserInfo> response = restTemplate.exchange(
			USER_INFO_URI, // https://www.googleapis.com/oauth2/v3/userinfo
			HttpMethod.GET,
			httpEntity,
			GoogleUserInfo.class
		);

		GoogleUserInfo googleUserInfo = response.getBody();

		if (!response.getStatusCode().is2xxSuccessful() || googleUserInfo == null) {
			throw new OAuthProcessingException("구글 사용자 정보 요청 실패");
		}
		return googleUserInfo;
	}
}
