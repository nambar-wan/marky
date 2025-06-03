package com.groom.marky.domain;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "users",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = {"userEmail", "loginType"})
	}
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String userEmail;

	// google oauth 로그인의 경우 패스워드가 비어있음.
	private String password;

	// 구글 로그인 유저만 값 있음
	private String googleId;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private LoginType loginType;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private Role role; // ROLE_USER, ROLE_ADMIN

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private int totalTokensUsed = 0;


	@Builder
	public User(String userEmail, String password, LoginType loginType, Role role, String name) {
		this.userEmail = userEmail;
		this.password = password;
		this.name = name;
		this.loginType = loginType;
		this.role = role;
	}

	public boolean isSamePassword(String rawPassword, PasswordEncoder passwordEncoder) {
		return passwordEncoder.matches(rawPassword, password);
	}
}
