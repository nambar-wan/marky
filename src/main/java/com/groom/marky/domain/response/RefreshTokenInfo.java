package com.groom.marky.domain.response;

import com.groom.marky.domain.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RefreshTokenInfo {

	private String refreshToken;
	private String userEmail;
	private long expiresAt;
	private String ip;
	private String userAgent;
	private Role role;
}
