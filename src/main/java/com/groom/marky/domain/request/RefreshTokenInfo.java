package com.groom.marky.domain.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenInfo {

	private String userEmail;
	private String refreshToken;
	private String ip;
	private String userAgent;
	private long expiresAt;
}
