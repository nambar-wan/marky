package com.groom.marky.common.security.jwt;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@SpringBootTest
@Import(JwtProvider.class)
class JwtProviderTest {

	@Autowired
	private JwtProvider jwtProvider;

	@Nested
	@DisplayName("JWT 토큰 관련 테스트")
	class JwtTokenTests {

		@DisplayName("토큰 생성 테스트")
		@Test
		void generateTokenTest() {
			// given
			String secret = "als;kdjf;asdjflaksjdf;laksjd;lfajsd;lfakjsd;lfjasldkfj;asdkjfa;lkjwe";
			SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
			String userEmail = "km1031kim@naver.com";

			// when
			String createdToken = jwtProvider.generateToken(userEmail);

			// then
			assertThat(createdToken).isNotNull();

			Claims claims = Jwts.parserBuilder()
				.setSigningKey(key).build()
				.parseClaimsJws(createdToken)
				.getBody();

			assertThat(claims.getSubject()).isEqualTo(userEmail);
			assertThat(claims.getExpiration()).isAfter(new Date());

		}

	}

}
