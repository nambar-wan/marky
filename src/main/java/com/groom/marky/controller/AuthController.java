package com.groom.marky.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.groom.marky.common.security.jwt.JwtProvider;
import com.groom.marky.domain.Role;
import com.groom.marky.domain.request.CreateUserRequest;
import com.groom.marky.domain.request.LoginRequest;
import com.groom.marky.domain.request.RefreshTokenInfo;
import com.groom.marky.domain.response.TokenResponse;
import com.groom.marky.domain.response.UserResponse;
import com.groom.marky.service.UserService;
import com.groom.marky.service.impl.RedisService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/auth")
public class AuthController {

	/**
	 * 0. 회원가입
	 * 1. 로그인 -> DB조회, 유저 확인 후 액세스 토큰, 리프레쉬 토큰 밝브 ->  레디스에 리프레쉬 토큰 저장(유저,토큰, Ip, 접근기기)
	 * 2. 로그아웃 -> 시큐리티 경로 지정. Authentication 삭제, 리프레쉬 토큰 삭제
	 * 3. 리프레쉬 토큰을 통한 액세스 토큰 재발급. -> 리프레쉬 토큰 검증(유저,토큰,Ip,접근기기) -> 액세스 토큰, 리프레쉬 토큰 재발급 -> 레디스에 리프레쉬 토큰 저장
	 * 4.
	 */

	private final UserService userService;
	private final RedisService redisService;
	private final JwtProvider jwtProvider;

	@Autowired
	public AuthController(UserService userService, RedisService redisService, JwtProvider jwtProvider) {
		this.userService = userService;
		this.redisService = redisService;
		this.jwtProvider = jwtProvider;
	}

	// 로컬 유저 회원가입
	@PostMapping("/signup")
	public ResponseEntity<?> register(@RequestBody @Valid CreateUserRequest request) {

		/**
		 * Validation 실패 시 MethodArgumentNotValidException 발생 -> ApiExceptionAdvice 에서 처리
		 */

		UserResponse response = userService.register(request);

		return ResponseEntity.ok(response);

	}

	// 로그인 -> 액세스 토큰, 리프레쉬 토큰 발급
	@PostMapping("/login")
	public ResponseEntity<?> localLogin(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) throws
		JsonProcessingException {

		// 아이디 패스워드 확인
		UserResponse userResponse = userService.validate(request);
		String userEmail = userResponse.getUserEmail();
		Role role = userResponse.getRole();

		// access token, refresh token 발급
		String accessToken = jwtProvider.generateAccessToken(userEmail, role);
		String refreshToken = jwtProvider.generateRefreshToken(userEmail, role);

		// refresh token 정보 레디스에 저장
		/**
		 * {
		 *   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
		 *   "ip": "192.168.0.3",
		 *   "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X...)",
		 *   "expiresAt": 1726508400000
		 * }
		 */
		String ip = httpRequest.getRemoteAddr();
		String userAgent = httpRequest.getHeader("User-Agent");
		long expiresAt = jwtProvider.getRefreshTokenExpiry(refreshToken);

		RefreshTokenInfo tokenInfo = RefreshTokenInfo.builder()
			.userEmail(userEmail)
			.refreshToken(refreshToken)
			.ip(ip)
			.userAgent(userAgent)
			.expiresAt(expiresAt)
			.build();

		redisService.setRefreshToken(tokenInfo);

		return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken));

	}
}
