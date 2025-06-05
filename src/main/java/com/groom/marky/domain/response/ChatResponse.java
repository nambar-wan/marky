package com.groom.marky.domain.response;

import java.util.List;

import lombok.Data;

@Data
public class ChatResponse {
	private String message;
	private List<PlaceResponse> places;
}
