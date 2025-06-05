package com.groom.marky.domain.request;

import com.groom.marky.domain.LoginType;
import com.groom.marky.domain.Role;
import com.groom.marky.domain.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {

	@Email(message = "이메일 형식이 유효하지 않습니다.")
	@NotBlank(message = "이메일은 필수입니다.")
	private String userEmail;

	@NotBlank(message = "비밀번호는 필수입니다.")
	private String password;

	@NotBlank(message = "이름을 필수입니다.")
	private String name;



	public User toUser() {
		return User.builder()
			.userEmail(userEmail)
			.name(name)
			.password(password)
			.role(Role.ROLE_USER)
			.loginType(LoginType.LOCAL)
			.build();
	}

}
