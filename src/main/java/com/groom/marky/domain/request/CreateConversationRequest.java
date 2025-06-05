package com.groom.marky.domain.request;

import com.groom.marky.domain.User;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateConversationRequest {

	private String conversationId;

	private String title;

	private String userEmail;

}
