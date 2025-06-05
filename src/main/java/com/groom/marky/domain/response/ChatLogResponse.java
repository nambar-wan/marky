package com.groom.marky.domain.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatLogResponse {

	private String question;
	private ChatResponse answer;

}
