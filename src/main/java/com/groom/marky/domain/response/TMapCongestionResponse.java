package com.groom.marky.domain.response;

import java.util.List;

import lombok.Data;

@Data
public class TMapCongestionResponse {
	private Status status;
	private Contents contents;

	@Data
	public static class Status {
		private String code;
		private String message;
		private int totalCount;
	}

	@Data
	public static class Contents {
		private String poiId;
		private String poiName;
		private List<Rltm> rltm;
	}

	@Data
	public static class Rltm {
		private int type;
		private double congestion;
		private int congestionLevel;
		private String datetime;
	}
}
