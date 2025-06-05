package com.groom.marky.domain.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequest {


	@Email(message = "이메일 형식이 유효하지 않습니다.")
	@NotBlank(message = "이메일은 필수입니다.")
	private String userEmail;


	@NotBlank(message = "비밀번호는 필수입니다.")
	private String password;
}
