package com.groom.marky.domain.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateConversationResponse {

	private String conversationId;

}
