package com.groom.marky.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateChatRequest {

	private String conversationId;

	@NotBlank
	private String message;

}
