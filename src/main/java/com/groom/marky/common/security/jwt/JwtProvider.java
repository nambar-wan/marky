package com.groom.marky.common.security.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.groom.marky.domain.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 생성, 검증, 유저 정보 추출 등 JWT 자체를 다루는 헬퍼 클래스
 */
@Slf4j
public class JwtProvider {

	private static final SignatureAlgorithm algorithm = SignatureAlgorithm.HS256;
	private final SecretKey accessSecret;
	private final SecretKey refreshSecret;
	private final long accessDuration;
	private final long refreshDuration;

	public JwtProvider(String accessKey, String refreshKey, long accessDuration, long refreshDuration) {
		accessSecret = Keys.hmacShaKeyFor(accessKey.getBytes(StandardCharsets.UTF_8));
		refreshSecret = Keys.hmacShaKeyFor(refreshKey.getBytes(StandardCharsets.UTF_8));
		this.accessDuration = accessDuration;
		this.refreshDuration = refreshDuration;
	}

	/**
	 * 액세스 토큰 생성 : 유저메일을 페이로드에 담기
	 */
	public String generateAccessToken(String userEmail, Role role) {
		// 1. 유효시간 설정
		Date now = new Date();

		return Jwts.builder()
			.setSubject(userEmail) // Subject 는 클레임의 일. sub : 주체, 대상, 토큰의 주인
			.claim("role", role.name())
			.setIssuedAt(now)
			.setExpiration(new Date(now.getTime() + accessDuration))
			.signWith(accessSecret, algorithm) // HMAC 계열 알고리즘
			.compact();
	}

	/**
	 *  리프레쉬 토큰 생성 : 유저메일을 페이로드에 담기
	 */
	public String generateRefreshToken(String userEmail, Role role) {
		Date now = new Date();

		return Jwts.builder()
			.setSubject(userEmail)
			.claim("role", role.name())
			.setIssuedAt(now)
			.setExpiration(new Date(now.getTime() + refreshDuration))
			.signWith(refreshSecret, algorithm)
			.compact();
	}

	/**
	 * 요청 헤더에서 JWT 추출 : Authorization 헤더로부터 추출
	 */
	public String resolveToken(HttpServletRequest request) {
		String bearer = request.getHeader("Authorization");
		if (bearer != null && bearer.startsWith("Bearer ")) {
			return bearer.substring(7);
		}
		return null;
	}

	/**
	 * accessToken 검증
	 * @param accessToken
	 * @return
	 */
	public boolean validateAccessToken(String accessToken) {
		try {
			Jwts.parserBuilder()
				.setSigningKey(accessSecret)
				.build()
				.parseClaimsJws(accessToken);
			return true;
		} catch (ExpiredJwtException e) {
			log.warn("JWT 토큰이 만료되었습니다: {}", e.getMessage());
		} catch (UnsupportedJwtException e) {
			log.warn("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
		} catch (MalformedJwtException e) {
			log.warn("JWT 형식이 올바르지 않습니다: {}", e.getMessage());
		} catch (SignatureException e) {
			log.warn("JWT 서명이 유효하지 않습니다: {}", e.getMessage());
		} catch (IllegalArgumentException e) {
			log.warn("JWT claims 문자열이 비었습니다: {}", e.getMessage());
		}
		return false;
	}

	/**
	 * refresh Token 검증
	 * @param refreshToken
	 * @return
	 */
	public boolean validateRefreshToken(String refreshToken) {
		try {
			Jwts.parserBuilder()
				.setSigningKey(refreshSecret)
				.build()
				.parseClaimsJws(refreshToken);
			return true;
		} catch (ExpiredJwtException e) {
			log.warn("JWT 토큰이 만료되었습니다: {}", e.getMessage());
		} catch (UnsupportedJwtException e) {
			log.warn("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
		} catch (MalformedJwtException e) {
			log.warn("JWT 형식이 올바르지 않습니다: {}", e.getMessage());
		} catch (SignatureException e) {
			log.warn("JWT 서명이 유효하지 않습니다: {}", e.getMessage());
		} catch (IllegalArgumentException e) {
			log.warn("JWT claims 문자열이 비었습니다: {}", e.getMessage());
		}
		return false;
	}

	/**
	 * JWT에서 유저 정보 추출 : Authentication 객체로 변환
	 */
	public Authentication getAuthentication(String accessToken) {

		// 1. subject 구하기
		Claims claims = Jwts.parserBuilder()
			.setSigningKey(accessSecret)
			.build()
			.parseClaimsJws(accessToken).getBody();

		String userEmail = claims.getSubject();

		String roleString = claims.get("role", String.class);
		Role role = Role.valueOf(roleString);

		List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role.name()));

		UserDetails userDetails = new User(userEmail, "", authorities);

		return new UsernamePasswordAuthenticationToken(userDetails, accessToken, userDetails.getAuthorities());
	}

	public String regenerateAccessToken(String refreshToken) {

		// 1. refresh token 검증 끝. 다시 만들기만 하면 됨.
		if (!validateRefreshToken(refreshToken)) {
			throw new IllegalArgumentException("invalid refresh token");
		}

		Claims claims = Jwts.parserBuilder()
			.setSigningKey(refreshSecret)
			.build()
			.parseClaimsJws(refreshToken)
			.getBody();

		String userEmail = claims.getSubject();
		String roleString = claims.get("role", String.class);
		Role role = Role.valueOf(roleString);

		return generateAccessToken(userEmail, role);
	}

	public long getRefreshTokenExpiry(String refreshToken) {
		Claims claims = Jwts.parserBuilder()
			.setSigningKey(refreshSecret)
			.build()
			.parseClaimsJws(refreshToken)
			.getBody();

		return claims.getExpiration().getTime();
	}

	public String getSubject(String accessToken) {

		if (!validateAccessToken(accessToken)) {
			throw new IllegalArgumentException("invalid access token");
		}

		Claims claims =
			Jwts.parserBuilder()
			.setSigningKey(accessSecret)
			.build().parseClaimsJws(accessToken).getBody();

		return claims.getSubject();
	}

	public Role getRole(String accessToken) {

		if (!validateAccessToken(accessToken)) {
			throw new IllegalArgumentException("invalid access token");
		}

		Claims claims =
			Jwts.parserBuilder()
				.setSigningKey(accessSecret)
				.build().parseClaimsJws(accessToken).getBody();

		String roleString = claims.get("role", String.class);

		return Role.valueOf(roleString);
	}
}
