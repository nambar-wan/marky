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
import com.groom.marky.domain.request.CreateToken;
import com.groom.marky.domain.response.AccessTokenInfo;
import com.groom.marky.domain.response.RefreshTokenInfo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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
	public String generateAccessToken(CreateToken createToken) {
		// 1. 유효시간 설정
		Date now = new Date();

		return Jwts.builder()
			.setSubject(createToken.getUserEmail()) // Subject 는 클레임의 일. sub : 주체, 대상, 토큰의 주인
			.claim("role", createToken.getRole().name())
			.claim("ip", createToken.getIp())
			.claim("userAgent", createToken.getUserAgent())
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
	public String resolveAccessToken(HttpServletRequest request) {
		String bearer = request.getHeader("Authorization");
		if (bearer != null && bearer.startsWith("Bearer ")) {
			return bearer.substring(7);
		}
		return null;
	}

	public String resolveRefreshToken(HttpServletRequest request) {
		String bearer = request.getHeader("Refresh-Token");
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

	public Claims getClaimsFromAccessToken(String accessToken) {
		try {
			return Jwts.parser()
				.setSigningKey(accessSecret)
				.parseClaimsJws(accessToken)
				.getBody();
		} catch (ExpiredJwtException e) {
			return e.getClaims();
		} catch (JwtException | IllegalArgumentException e) {
			throw new JwtException("토큰 파싱 실패", e);
		}
	}



	public long getRefreshTokenExpiry(String refreshToken) {
		Claims claims = Jwts.parserBuilder()
			.setSigningKey(refreshSecret)
			.build()
			.parseClaimsJws(refreshToken)
			.getBody();

		return claims.getExpiration().getTime();
	}

	public long getAccessTokenExpiry(String accessToken) {
		log.info("getAccessTokenExpiry 진입");
		Claims claims = Jwts.parserBuilder()
			.setSigningKey(accessSecret)
			.build()
			.parseClaimsJws(accessToken) // 예외 발생 가능. 토큰 만료되었거나 등등..
			.getBody();

		log.info("getAccessTokenExpiry 탈출");

		return claims.getExpiration().getTime();
	}

	public String getSubjectFromAccessToken(String accessToken) {

		if (!validateAccessToken(accessToken)) {
			throw new IllegalArgumentException("invalid access token");
		}

		Claims claims =
			Jwts.parserBuilder()
			.setSigningKey(accessSecret)
			.build().parseClaimsJws(accessToken).getBody();

		return claims.getSubject();
	}

	public String getSubjectFromRefreshToken(String refreshToken) {

		if (!validateRefreshToken(refreshToken)) {
			throw new IllegalArgumentException("invalid access token");
		}

		Claims claims =
			Jwts.parserBuilder()
				.setSigningKey(refreshSecret)
				.build().parseClaimsJws(refreshToken).getBody();

		return claims.getSubject();
	}

	public Role getRoleFromAccessToken(String accessToken) {

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


	public Role getRoleFromRefreshToken(String refreshToken) {

		if (!validateRefreshToken(refreshToken)) {
			throw new IllegalArgumentException("invalid refresh token");
		}

		Claims claims =
			Jwts.parserBuilder()
				.setSigningKey(refreshSecret)
				.build().parseClaimsJws(refreshToken).getBody();

		String roleString = claims.get("role", String.class);

		return Role.valueOf(roleString);
	}

	// 검증은 해당 메서드 역할이 아니다.
	public AccessTokenInfo getAccessTokenInfo(String accessToken) {

		Claims claims =	Jwts.parserBuilder()
							.setSigningKey(accessSecret)
							.build().parseClaimsJws(accessToken).getBody();

		String userEmail = claims.getSubject();
		String ip = claims.get("ip", String.class);
		Role role = Role.valueOf(claims.get("role", String.class));
		String userAgent = claims.get("userAgent", String.class);
		long expireAt = claims.getExpiration().getTime();

		return AccessTokenInfo.builder()
			.userEmail(userEmail)
			.ip(ip)
			.role(role)
			.userAgent(userAgent)
			.expiresAt(expireAt)
			.build();

	}

	// 검증은 해당 메서드 역할이 아니다.
	public RefreshTokenInfo getRefreshTokenInfo(String refreshToken) {

		Claims claims =	Jwts.parserBuilder()
			.setSigningKey(refreshSecret)
			.build().parseClaimsJws(refreshToken).getBody();

		String userEmail = claims.getSubject();
		Role role = Role.valueOf(claims.get("role", String.class));
		String ip = claims.get("ip",String.class);
		String userAgent = claims.get("userAgent",String.class);

		long expireAt = claims.getExpiration().getTime();

		RefreshTokenInfo build = RefreshTokenInfo.builder()
			.refreshToken(refreshToken)
			.userEmail(userEmail)
			.ip(ip)
			.role(role)
			.userAgent(userAgent)
			.expiresAt(expireAt)
			.build();

		log.info("build : {}", build);
		return build;

	}

	public boolean validateAccessTokenAllowExpired(String accessToken) {
		try {
			Jwts.parserBuilder()
				.setSigningKey(accessSecret)
				.build()
				.parseClaimsJws(accessToken);
			return true;
		} catch (ExpiredJwtException e) {
			log.warn("JWT 토큰이 만료되었지만 서명은 유효합니다: {}", e.getMessage());
			return true;
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

	public Claims getClaimsFromAllowedExpiredAccessToken(String accessToken) {
		try {
			return Jwts.parserBuilder()
				.setSigningKey(accessSecret)
				.build()
				.parseClaimsJws(accessToken).getBody();
		} catch (ExpiredJwtException e) {
			log.warn("JWT 토큰이 만료되었지만 서명은 유효합니다: {}", e.getMessage());
			return e.getClaims();
		} catch (UnsupportedJwtException e) {
			log.warn("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
		} catch (MalformedJwtException e) {
			log.warn("JWT 형식이 올바르지 않습니다: {}", e.getMessage());
		} catch (SignatureException e) {
			log.warn("JWT 서명이 유효하지 않습니다: {}", e.getMessage());
		} catch (IllegalArgumentException e) {
			log.warn("JWT claims 문자열이 비었습니다: {}", e.getMessage());
		}
		throw new JwtException("유효하지 않은 Access Token 입니다.");
	}

}
