package com.groom.marky.common;

import com.groom.marky.common.constant.GooglePlaceType;

public class RedisKeyParser {

	private static final String PLACES = "places";

	public static String getPlaceKey(GooglePlaceType type) {
		return PLACES + ":" + type.getGoogleType();
	}
}
