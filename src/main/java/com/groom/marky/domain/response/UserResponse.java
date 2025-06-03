package com.groom.marky.domain.response;

import com.groom.marky.domain.LoginType;
import com.groom.marky.domain.Role;
import com.groom.marky.domain.User;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Builder
@Data
public class UserResponse {

	private Long id;
	private String userEmail;
	private String name;
	private LoginType loginType;
	private Role role;
	private int totalTokensUsed;

	public static UserResponse from(User user) {

		return UserResponse.builder()
			.id(user.getId())
			.userEmail(user.getUserEmail())
			.name(user.getName())
			.loginType(user.getLoginType())
			.role(user.getRole())
			.totalTokensUsed(user.getTotalTokensUsed())
			.build();
	}
}
