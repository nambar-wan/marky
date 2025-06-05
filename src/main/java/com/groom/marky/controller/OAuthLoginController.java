package com.groom.marky.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.groom.marky.domain.Role;
import com.groom.marky.domain.request.CreateToken;
import com.groom.marky.domain.response.GoogleUserInfo;
import com.groom.marky.domain.response.TokenResponse;
import com.groom.marky.domain.response.UserResponse;
import com.groom.marky.service.UserService;
import com.groom.marky.service.impl.GoogleOAuthService;
import com.groom.marky.service.impl.JwtService;
import com.groom.marky.service.impl.RedisService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/auth")
public class OAuthLoginController {

	private final GoogleOAuthService googleOAuthService;
	private final UserService userService;
	private final JwtService jwtService;
	private final RedisService redisService;

	@Autowired
	public OAuthLoginController(GoogleOAuthService googleOAuthService, UserService userService, JwtService jwtService,
		RedisService redisService) {
		this.googleOAuthService = googleOAuthService;
		this.userService = userService;
		this.jwtService = jwtService;
		this.redisService = redisService;
	}

	@GetMapping("/google/login")
	public void redirectToGoogle(HttpServletResponse response) throws IOException {

		String loginUri = googleOAuthService.getLoginUri();
		response.sendRedirect(loginUri);
	}

	// http://localhost:8080/auth/google/callback
	@GetMapping("/google/callback")
	public ResponseEntity<?> callback(@RequestParam String code, HttpServletRequest request) throws
		JsonProcessingException {
		log.info("Google OAuth 인증 코드 수신: {}", code);


		// 1. code → access token 요청
		String accessToken = googleOAuthService.getAccessToken(code);

		// 2. access token → 사용자 정보 요청
		GoogleUserInfo userInfo = googleOAuthService.getUserInfo(accessToken);

		// 3. DB 조회 or 회원가입
		UserResponse userResponse = userService.findOrCreate(userInfo);


		// 4. JWT 토큰 반환
		String ip = request.getRemoteAddr();
		String userAgent = request.getHeader("User-Agent");
		String userEmail = userResponse.getUserEmail();
		Role role = userResponse.getRole();

		log.info("ip : {}, userAgent : {}", ip, userAgent);

		CreateToken createToken = CreateToken.builder()
			.ip(ip)
			.userAgent(userAgent)
			.userEmail(userEmail)
			.role(role)
			.build();

		TokenResponse tokens = jwtService.getTokens(createToken);

		// 5. 레디스 저장
		redisService.setRefreshToken(jwtService.getRefreshTokenInfo(tokens));

		return ResponseEntity.ok(tokens);
	}

}
