package com.groom.marky.common.security.jwt;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtProvider jwtProvider;

	@Autowired
	public JwtAuthenticationFilter(JwtProvider jwtProvider) {
		this.jwtProvider = jwtProvider;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return path.startsWith("/auth"); // 필터 적용 제외 경로
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		/**
		 * 1. request 에서 토큰 추출 -> 유저이름, 롤 확인
		 * 2. 액세스 토큰 검증
		 * 3. 액세스 토큰 검증 실패 시 예외 전달.
		 * 4. 액세스 토큰 검증 성공 시 Authentication 객체 생성. SecurityContext에 저장. -> 다음 체인 호출
		 */

		// 1. 토큰 추출
		String accessToken = jwtProvider.resolveToken(request);

		// 2. 토큰 검증
		boolean isValid = jwtProvider.validateAccessToken(accessToken);

		// 3. 토큰 검증 실패 시 예외 전달
		if (!isValid) {
			throw new IllegalArgumentException("invalid access token");
		}

		// 4. 토큰에 포함된 userEmail, userRole 을 바탕으로 Authentication 객체 생성
		Authentication authentication = jwtProvider.getAuthentication(accessToken);

		// 5. SecurityContext 등록 후 다음 체인 호출
		SecurityContextHolder.getContext().setAuthentication(authentication);

		filterChain.doFilter(request, response);

	}
}
