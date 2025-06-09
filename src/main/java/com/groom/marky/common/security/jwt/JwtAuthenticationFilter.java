package com.groom.marky.common.security.jwt;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.groom.marky.domain.response.RefreshTokenInfo;
import com.groom.marky.service.impl.RedisService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtProvider jwtProvider;
	private final RedisService redisService;

	@Autowired
	public JwtAuthenticationFilter(JwtProvider jwtProvider, RedisService redisService) {
		this.jwtProvider = jwtProvider;
		this.redisService = redisService;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return path.startsWith("/auth") || path.startsWith("/actuator");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
		FilterChain filterChain) throws ServletException, IOException {

		/**
		 * JwtAuthenticationFilter 흐름
		 * 1. httpRequest 로부터 토큰 추출,
		 * 2. 액세스 토큰이 블랙리스트에 등록되었는지 여부 확인
		 * 3. 토큰 검증 실패 시 401 예외 반환
		 * 4. 액세스 토큰으로부터 클레임 추출. 레디스 내부에 저장된 리프레쉬 토큰 정보와 비교 (해당 유저에게 발급된 리프레쉬 토큰은 레디스에 저장되어 있음.)
		 * 5. 검증 실패 시 401 예외 반환, 검증 완료 시 Authencation 객체 생성. 시큐리티 컨텍스트 등록
		 */

		// 1. 토큰 추출
		String accessToken = jwtProvider.resolveAccessToken(httpRequest);

		if (accessToken == null) {
			// 쿠키에서 accessToken 찾기
			if (httpRequest.getCookies() != null) {
				for (Cookie cookie : httpRequest.getCookies()) {
					if ("accessToken".equals(cookie.getName())) {
						accessToken = cookie.getValue();
						break;
					}
				}
			}
		}


		boolean isValid = jwtProvider.validateAccessToken(accessToken);

		// 2. 블랙리스트 여부 확인
		boolean isBlacklisted = redisService.isInBlacklist(accessToken);

		// 3. 토큰 검증 실패 시 예외 전달
		if (!isValid || isBlacklisted) {
			httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
			httpResponse.setContentType("application/json;charset=UTF-8");
			httpResponse.getWriter().write("{\"message\": \"유효하지 않거나 로그아웃된 토큰입니다.\"}");
			return;
		}

		// 5. 레디스에 저장된 리프레쉬 토큰 내부 클레임과 비교
		String userEmail = jwtProvider.getSubjectFromAccessToken(accessToken);
		RefreshTokenInfo refreshTokenFromRedis = redisService.getRefreshToken(userEmail);

		boolean ipMatch = httpRequest.getRemoteAddr().equals(refreshTokenFromRedis.getIp());
		boolean agentMatch = httpRequest.getHeader("User-Agent").equals(refreshTokenFromRedis.getUserAgent());

		if (!ipMatch || !agentMatch) {
			httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
			httpResponse.setContentType("application/json;charset=UTF-8");
			httpResponse.getWriter().write("{\"message\": \"레디스 내부에 저장된 리프레쉬 토큰 정보와 액세스 토큰 정보가 일치하지 않습니다.\"}");
			return;
		}


		// 6. 토큰에 포함된 userEmail, userRole 을 바탕으로 Authentication 객체 생성
		Authentication authentication = jwtProvider.getAuthentication(accessToken);

		// 7. SecurityContext 등록 후 다음 체인 호출
		SecurityContextHolder.getContext().setAuthentication(authentication);

		filterChain.doFilter(httpRequest, httpResponse);

	}
}
