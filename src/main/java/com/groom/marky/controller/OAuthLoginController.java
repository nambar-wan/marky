package com.groom.marky.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import com.groom.marky.domain.request.CreateToken;
import com.groom.marky.domain.response.GoogleUserInfo;
import com.groom.marky.domain.response.TokenResponse;
import com.groom.marky.domain.response.UserResponse;
import com.groom.marky.service.UserService;
import com.groom.marky.service.impl.GoogleOAuthService;
import com.groom.marky.service.impl.JwtService;

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

	@Autowired
	public OAuthLoginController(GoogleOAuthService googleOAuthService, UserService userService, JwtService jwtService) {
		this.googleOAuthService = googleOAuthService;
		this.userService = userService;
		this.jwtService = jwtService;
	}

	@GetMapping("/google/login")
	public void redirectToGoogle(HttpServletResponse response) throws IOException {

		String loginUri = googleOAuthService.getLoginUri();
		response.sendRedirect(loginUri);
	}


	@GetMapping("/google/callback")
	public ResponseEntity<?> callback(@RequestParam String code, HttpServletRequest request) {
		log.info("Google OAuth 인증 코드 수신: {}", code);

		// 1. code → access token 요청
		String accessToken = googleOAuthService.getAccessToken(code);

		// 2. access token → 사용자 정보 요청
		GoogleUserInfo userInfo = googleOAuthService.getUserInfo(accessToken);

		//  3. DB 조회 or 회원가입
		UserResponse userResponse = userService.findOrCreate(userInfo);

		// 4. 토큰 반환
		CreateToken createToken = CreateToken.builder()
			.ip(request.getRemoteAddr())
			.userAgent(request.getHeader("User-Agent"))
			.userEmail(userResponse.getUserEmail())
			.role(userResponse.getRole())
			.build();

		TokenResponse tokens = jwtService.getTokens(createToken);

		return ResponseEntity.ok(tokens);
	}

}
