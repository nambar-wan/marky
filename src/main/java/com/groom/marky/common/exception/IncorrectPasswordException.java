package com.groom.marky.common.exception;

public class IncorrectPasswordException extends RuntimeException {

	public IncorrectPasswordException() {
		super("패스워드가 일치하지 않습니다.");
	}
}
