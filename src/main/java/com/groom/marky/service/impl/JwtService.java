package com.groom.marky.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.groom.marky.common.security.jwt.JwtProvider;
import com.groom.marky.domain.Role;
import com.groom.marky.domain.request.CreateToken;
import com.groom.marky.domain.response.AccessTokenInfo;
import com.groom.marky.domain.response.RefreshTokenInfo;
import com.groom.marky.domain.response.TokenResponse;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JwtService {

	private final JwtProvider jwtProvider;

	@Autowired
	public JwtService(JwtProvider jwtProvider) {
		this.jwtProvider = jwtProvider;
	}

	public TokenResponse getTokens(CreateToken createToken) {

		String accessToken = jwtProvider.generateAccessToken(createToken);
		String refreshToken = jwtProvider.generateRefreshToken(createToken.getUserEmail(), createToken.getRole());

		return new TokenResponse(accessToken, refreshToken);
	}

	public long getRefreshTokenExpiry(String refreshToken) {
		return jwtProvider.getRefreshTokenExpiry(refreshToken);
	}

	// 액세스 토큰, 리프레쉬 토큰 검증
	public TokenResponse validateToken(HttpServletRequest httpRequest) {
		String accessToken = jwtProvider.resolveAccessToken(httpRequest);
		String refreshToken = jwtProvider.resolveRefreshToken(httpRequest);

		boolean isAccessValid = jwtProvider.validateAccessToken(accessToken);
		boolean isRefreshValid = jwtProvider.validateRefreshToken(refreshToken);

		if (!isAccessValid || !isRefreshValid) {
			throw new JwtException("토큰이 유효하지 않습니다.");
		}

		return new TokenResponse(accessToken, refreshToken);
	}

	// 사용자가 보낸 액세스 토큰 클레임과, 레디스로부터 추출한  저장된 리프레쉬 토큰 정보 비교
	public boolean validateClaims(AccessTokenInfo accessTokenInfoFromUser, RefreshTokenInfo refreshTokenInfoFromRedis) {
		log.info("호출");
		String accessTokenIpFromUser = accessTokenInfoFromUser.getIp();
		String accessTokenAgentFromUser = accessTokenInfoFromUser.getUserAgent();
		String accessTokenUserEmailFromUser = accessTokenInfoFromUser.getUserEmail();
		Role accessTokenRoleFromUser = accessTokenInfoFromUser.getRole();

		boolean ipMatch = refreshTokenInfoFromRedis.getIp().equals(accessTokenIpFromUser);
		boolean agentMatch = refreshTokenInfoFromRedis.getUserAgent().equals(accessTokenAgentFromUser);
		boolean emailMatch = refreshTokenInfoFromRedis.getUserEmail().equals(accessTokenUserEmailFromUser);
		boolean roleMatch = refreshTokenInfoFromRedis.getRole().equals(accessTokenRoleFromUser);

		if (!ipMatch || !agentMatch || !emailMatch || !roleMatch) {
			return false;
		}

		return true;
	}

	public AccessTokenInfo getAccessTokenInfo(String accessToken) {
		return jwtProvider.getAccessTokenInfo(accessToken);
	}

	public TokenResponse validateTokenAllowExpired(HttpServletRequest httpRequest) {
		String accessToken = jwtProvider.resolveAccessToken(httpRequest);
		String refreshToken = jwtProvider.resolveRefreshToken(httpRequest);

		boolean isAccessValid = jwtProvider.validateAccessTokenAllowExpired(accessToken);
		boolean isRefreshValid = jwtProvider.validateRefreshToken(refreshToken);

		if (!isRefreshValid || !isAccessValid) {
			throw new JwtException("토큰이 유효하지 않습니다.");
		}

		return new TokenResponse(accessToken, refreshToken);

	}

	// 만료된 토큰도 꺼낼 수 있음
	public AccessTokenInfo getAllowedExpiredAccessTokenInfo(String accessToken) {

		Claims claims = jwtProvider.getClaimsFromAllowedExpiredAccessToken(accessToken);

		/**
		 * private String accessToken;
		 * 	private String userEmail;
		 * 	private long expiresAt;
		 * 	private String ip;
		 * 	private String userAgent;
		 * 	private Role role;
		 */
		return AccessTokenInfo.builder()
			.accessToken(accessToken)
			.userEmail(claims.getSubject())
			.expiresAt(claims.getExpiration().getTime())
			.ip(claims.get("ip", String.class))
			.userAgent(claims.get("userAgent", String.class))
			.role(Role.valueOf(claims.get("role", String.class)))
			.build();
	}
}
