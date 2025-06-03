package com.groom.marky.common.exception;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class ApiExceptionAdvice {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
		List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
			.map(error -> Map.of(
				"field", error.getField(),
				"message", error.getDefaultMessage()
			)).toList();

		return ResponseEntity.badRequest().body(Map.of("errors", errors));
	}

	@ExceptionHandler(UserAlreadyExistException.class)
	public ResponseEntity<?> handleUserAlreadyExist(UserAlreadyExistException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(Map.of(
				"message", e.getMessage()
			));

	}

	@ExceptionHandler(IncorrectPasswordException.class)
	public ResponseEntity<?> handleIncorrectPassword(IncorrectPasswordException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(Map.of(
				"message", e.getMessage()
			));
	}

	@ExceptionHandler(JsonProcessingException.class)
	public ResponseEntity<?> handleJsonProcessing(JsonProcessingException e) {
		return ResponseEntity.badRequest()
			.body(Map.of("message", "JSON 처리 중 오류가 발생했습니다.", "detail", e.getOriginalMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
		return ResponseEntity.badRequest()
			.body(Map.of("message", e.getMessage()));
	}


	@ExceptionHandler(JwtException.class)
	public ResponseEntity<?> handleJwt(JwtException e) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(Map.of("message", e.getMessage()));
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<?> handleEntityNotFound(EntityNotFoundException e) {
		return ResponseEntity.badRequest()
			.body(Map.of("message", e.getMessage()));
	}

}
