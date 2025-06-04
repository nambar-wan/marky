package com.groom.marky.common.exception;

import com.groom.marky.domain.LoginType;

public class UserAlreadyExistException extends RuntimeException {

	public UserAlreadyExistException(String userEmail, LoginType loginType) {
		super(loginType.name() + " 로그인 방식의 이메일이 이미 존재합니다: " + userEmail);
	}
}
