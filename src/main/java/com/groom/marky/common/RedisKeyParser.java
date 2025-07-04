package com.groom.marky.common;

import com.groom.marky.common.constant.GooglePlaceType;

public class RedisKeyParser {

	private static final String PLACES = "places";
	private static final String REFRESH_TOKEN = "refresh";
	private static final String BLACKLIST = "blacklist";

	public static String getPlaceKey(GooglePlaceType type) {
		return PLACES + ":" + type.getGoogleType();
	}

	public static String getRefreshTokenKey(String userEmail) {
		return REFRESH_TOKEN + ":" + userEmail;
	}

	public static String getBlacklistKey(String accessToken) {
		return BLACKLIST + ":" + accessToken;
	}
}
