package com.groom.marky.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.groom.marky.common.security.jwt.JwtProvider;
import com.groom.marky.domain.Role;
import com.groom.marky.domain.request.CreateToken;
import com.groom.marky.domain.request.CreateUserRequest;
import com.groom.marky.domain.request.LoginRequest;
import com.groom.marky.domain.response.AccessTokenInfo;
import com.groom.marky.domain.response.RefreshTokenInfo;
import com.groom.marky.domain.response.TokenResponse;
import com.groom.marky.domain.response.UserResponse;
import com.groom.marky.service.UserService;
import com.groom.marky.service.impl.JwtService;
import com.groom.marky.service.impl.RedisService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/auth")
public class AuthController {

	/**
	 * 0. 회원가입
	 * 1. 로그인 -> DB조회, 유저 확인 후 액세스 토큰, 리프레쉬 토큰 밝브 ->  레디스에 리프레쉬 토큰 저장(유저,토큰, Ip, 접근기기)
	 * 2. 로그아웃 -> 시큐리티 경로 지정. Authentication 삭제, 리프레쉬 토큰 삭제
	 * 3. 리프레쉬 토큰을 통한 액세스 토큰 재발급. -> 리프레쉬 토큰 검증(유저,토큰,Ip,접근기기) -> 액세스 토큰, 리프레쉬 토큰 재발급 -> 레디스에 리프레쉬 토큰 저장
	 */

	private final UserService userService;
	private final RedisService redisService;
	private final JwtService jwtService;

	@Autowired
	public AuthController(UserService userService, RedisService redisService, JwtProvider jwtProvider,
		JwtService jwtService) {
		this.userService = userService;
		this.redisService = redisService;
		this.jwtService = jwtService;
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

	/**
	 * 로그인 메서드 : 액세스 토큰, 리프레쉬 토큰 재발급
	 * 1. 아이디 패스워드 확인
	 * 2. 유저 정보 및 요청 정보 추출
	 * 3. 액세스 토큰 및 리프레쉬 토큰 발급.
	 * 4. 레디스에 저장된 이존 리프레쉬 토큰 삭제
	 * 5. 레디스에 신규 리프레쉬 토큰 저장 - ip, agent, role, userEmail
	 * 6. 액세스 토큰 및 리프레쉬 토큰 반환
	 *
	 * 같은 아이피, 같은 기기에서 재접속하게되면 문제가 있음.
	 *
	 * 액세스 토큰 및 리프레쉬 토큰이 재발급 됨.
	 *
	 *
	 * @param loginRequest 로그인 정보
	 * @param httpRequest 요청 정보
	 * @return
	 * @throws JsonProcessingException
	 */
	@PostMapping("/login")
	public ResponseEntity<?> localLogin(@RequestBody @Valid LoginRequest loginRequest,
		HttpServletRequest httpRequest) throws
		JsonProcessingException {

		// 아이디 패스워드 확인
		UserResponse userResponse = userService.validate(loginRequest);

		String userEmail = userResponse.getUserEmail();
		Role role = userResponse.getRole();
		String ip = httpRequest.getRemoteAddr();
		String userAgent = httpRequest.getHeader("User-Agent");
		CreateToken createToken = new CreateToken(userEmail, role, ip, userAgent);

		// access token, refresh token 발급
		TokenResponse tokens = jwtService.getTokens(createToken);
		String accessToken = tokens.getAccessToken();
		String refreshToken = tokens.getRefreshToken();
		long refreshTokenExpiry = jwtService.getRefreshTokenExpiry(refreshToken);

		// redis 에 기존에 존재하던 refresh token 삭제
		redisService.deleteRefreshTokenByKey(userEmail);

		// redis 에 refresh token 저장.
		RefreshTokenInfo tokenInfo = RefreshTokenInfo.builder()
			.ip(ip)
			.userAgent(userAgent)
			.userEmail(userEmail)
			.role(role)
			.refreshToken(refreshToken)
			.expiresAt(refreshTokenExpiry)
			.build();

		redisService.setRefreshToken(tokenInfo);

		return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken));
	}

	@PostMapping("/webclient-login")
	public ResponseEntity<?> webClientLogin(@RequestBody @Valid LoginRequest loginRequest,
		HttpServletRequest httpRequest,
		HttpServletResponse httpResponse) throws IOException {

		// 1. 유저 검증
		UserResponse userResponse = userService.validate(loginRequest);

		String userEmail = userResponse.getUserEmail();
		Role role = userResponse.getRole();
		String ip = httpRequest.getRemoteAddr();
		String userAgent = httpRequest.getHeader("User-Agent");

		// 2. 토큰 생성
		CreateToken createToken = new CreateToken(userEmail, role, ip, userAgent);
		TokenResponse tokens = jwtService.getTokens(createToken);
		String accessToken = tokens.getAccessToken();
		String refreshToken = tokens.getRefreshToken();
		long refreshTokenExpiry = jwtService.getRefreshTokenExpiry(refreshToken);

		// 3. Redis 저장
		redisService.deleteRefreshTokenByKey(userEmail);
		redisService.setRefreshToken(RefreshTokenInfo.builder()
			.ip(ip)
			.userAgent(userAgent)
			.userEmail(userEmail)
			.role(role)
			.refreshToken(refreshToken)
			.expiresAt(refreshTokenExpiry)
			.build());


		// **수정 부분:** 토큰을 HttpOnly 쿠키로 설정하고 응답 본문 없이 반환
		ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
			.httpOnly(true)
			.secure(false) // HTTPS 배포 시 true
			.path("/")
			.domain("localhost") // 개발 환경 고려
			.sameSite("Lax")
			.maxAge(900) // 15분
			.build();

		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
			.httpOnly(true)
			.secure(false)
			.path("/")
			.domain("localhost") // 개발 환경 고려
			.sameSite("Lax")
			.maxAge((int) refreshTokenExpiry)
			.build();

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
		headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());

		log.info("header : {} ", headers);

		// 5. 응답은 바디 없이 200 OK와 함께 쿠키 헤더 반환
		return ResponseEntity.ok().headers(headers).build();

	}

	/**
	 * 로그아웃
	 * 1. 액세스 토큰, 리프레쉬 토큰 검증
	 * 2. 사용자가 보낸 리프레쉬 토큰이 레디스에 있는지 확인
	 * 3. 사용자가 보낸 액세스 토큰 값과, 리프레쉬 토큰 값 검증
	 * 4. 검증 완료 시 리프레쉬 토큰 삭제
	 * 5. 액세스 토큰은 리프레쉬 토큰 ttl만큼 블랙리스트 등록
	 * 6. 시큐리티 컨텍스트에서 유저정보 제거
	 * @param httpRequest
	 * @return
	 * @throws JsonProcessingException
	 */
	// /auth/logout
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws JsonProcessingException {

		log.info("로그아웃 요청");

		// 액세스 토큰, 리프레쉬 토큰 검증 후 추출
		String accessToken = resolveAccessTokenFromHeaderOrCookie(httpRequest);
		String refreshToken = resolveRefreshTokenFromHeaderOrCookie(httpRequest);

		if (accessToken == null || refreshToken == null) {
			throw new JwtException("토큰이 없습니다.");
		}

		// 액세스 토큰 정보 추출
		AccessTokenInfo accessTokenInfo = jwtService.getAccessTokenInfo(accessToken);

		// 액세스 토큰으로부터 유저이메일 획득
		String userEmail = accessTokenInfo.getUserEmail();

		// 유저 이메일로 레디스에 저장된 리프레쉬 토큰 획득
		RefreshTokenInfo refreshTokenInfoFromRedis = redisService.getRefreshToken(userEmail);

		// 사용자가 보낸 리프레쉬 토큰과, 레디스 내부에 저장된 리프레쉬 토큰 스트링 비교
		if (!refreshToken.equals(refreshTokenInfoFromRedis.getRefreshToken())) {
			throw new JwtException("리프레쉬 토큰이 유효하지 않습니다.");
		}

		// 사용자가 보낸 액세스 토큰 클레임과, 레디스로부터 추출한  저장된 리프레쉬 토큰 정보 비교
		boolean isValid = jwtService.validateClaims(accessTokenInfo, refreshTokenInfoFromRedis);

		if (!isValid) {
			throw new JwtException("토큰 정보가 유효하지 않습니다.");
		}

		// 검증 완료. 리프레쉬 토큰 삭제 및 남은 액세스 토큰 블랙리스트 등록.
		redisService.deleteRefreshToken(refreshTokenInfoFromRedis);
		redisService.registerBlacklist(accessToken, accessTokenInfo.getExpiresAt());

		// 시큐리티 컨텍스트에서 유저정보 제거
		SecurityContextHolder.clearContext();

		// 쿠키 삭제
		ResponseCookie clearAccess = ResponseCookie.from("accessToken", "")
			.path("/")
			.httpOnly(true)
			.secure(false)  // HTTPS 환경일 경우 true
			.maxAge(0)
			.build();

		ResponseCookie clearRefresh = ResponseCookie.from("refreshToken", "")
			.path("/")
			.httpOnly(true)
			.secure(false)
			.maxAge(0)
			.build();

		httpResponse.addHeader("Set-Cookie", clearAccess.toString());
		httpResponse.addHeader("Set-Cookie", clearRefresh.toString());

		return ResponseEntity.ok().build();
	}


	@PostMapping("/token/refresh")
	public ResponseEntity<?> reissueAccessToken(HttpServletRequest httpRequest,
		HttpServletResponse httpResponse) throws JsonProcessingException {

		// 쿠키 또는 헤더에서 accessToken, refreshToken 추출
		String accessToken = resolveAccessTokenFromHeaderOrCookie(httpRequest);
		String refreshToken = resolveRefreshTokenFromHeaderOrCookie(httpRequest);

		if (accessToken == null || refreshToken == null) {
			throw new JwtException("토큰이 없습니다.");
		}

		// 블랙리스트 확인
		if (redisService.isInBlacklist(accessToken)) {
			throw new JwtException("블랙리스트에 등록된 액세스 토큰입니다.");
		}

		// 기존 accessToken의 클레임 파싱 (만료 허용)
		AccessTokenInfo accessTokenInfo = jwtService.getAllowedExpiredAccessTokenInfo(accessToken);

		// Redis에 저장된 refreshToken 정보 조회
		RefreshTokenInfo refreshTokenInfoFromRedis = redisService.getRefreshToken(accessTokenInfo.getUserEmail());

		if (!refreshToken.equals(refreshTokenInfoFromRedis.getRefreshToken())) {
			throw new JwtException("리프레쉬 토큰이 유효하지 않습니다.");
		}

		if (!jwtService.validateClaims(accessTokenInfo, refreshTokenInfoFromRedis)) {
			throw new JwtException("토큰 정보가 유효하지 않습니다.");
		}

		// 새 토큰 발급
		String userEmail = accessTokenInfo.getUserEmail();
		String requestIp = httpRequest.getRemoteAddr();
		String requestUserAgent = httpRequest.getHeader("User-Agent");
		Role role = accessTokenInfo.getRole();

		CreateToken createToken = new CreateToken(userEmail, role, requestIp, requestUserAgent);
		TokenResponse newTokens = jwtService.getTokens(createToken);

		String newAccessToken = newTokens.getAccessToken();
		String newRefreshToken = newTokens.getRefreshToken();
		long refreshTokenExpiry = jwtService.getRefreshTokenExpiry(newRefreshToken);

		// Redis 업데이트
		redisService.deleteRefreshToken(refreshTokenInfoFromRedis);
		redisService.setRefreshToken(RefreshTokenInfo.builder()
			.ip(requestIp)
			.role(role)
			.userAgent(requestUserAgent)
			.refreshToken(newRefreshToken)
			.userEmail(userEmail)
			.expiresAt(refreshTokenExpiry)
			.build());

		// 기존 accessToken은 블랙리스트에 등록
		redisService.registerBlacklist(accessToken, accessTokenInfo.getExpiresAt());

		// 새 토큰을 HttpOnly 쿠키로 설정
		ResponseCookie accessCookie = ResponseCookie.from("accessToken", newAccessToken)
			.httpOnly(true)
			.secure(false) // HTTPS 환경에선 true
			.path("/")
			.sameSite("Lax")
			.maxAge(900)
			.build();

		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefreshToken)
			.httpOnly(true)
			.secure(false)
			.path("/")
			.sameSite("Lax")
			.maxAge((int) refreshTokenExpiry)
			.build();

		httpResponse.addHeader("Set-Cookie", accessCookie.toString());
		httpResponse.addHeader("Set-Cookie", refreshCookie.toString());

		return ResponseEntity.ok().body("{\"message\": \"토큰이 재발급되었습니다.\"}");
	}

	private String resolveAccessTokenFromHeaderOrCookie(HttpServletRequest request) {
		String bearer = request.getHeader("Authorization");
		if (bearer != null && bearer.startsWith("Bearer ")) {
			return bearer.substring(7);
		}

		// 헤더 없으면 쿠키에서 찾기
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if ("accessToken".equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}

		return null;
	}

	private String resolveRefreshTokenFromHeaderOrCookie(HttpServletRequest request) {
		String refreshToken = request.getHeader("X-Refresh-Token");

		if (refreshToken != null && !refreshToken.isBlank()) {
			return refreshToken;
		}

		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if ("refreshToken".equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}

		return null;
	}

}
