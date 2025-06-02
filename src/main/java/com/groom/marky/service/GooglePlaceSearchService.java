package com.groom.marky.service;

import java.util.List;
import java.util.Set;

import com.groom.marky.common.constant.GooglePlaceType;
import com.groom.marky.domain.request.Rectangle;
import com.groom.marky.domain.response.GooglePlacesApiResponse;

public interface GooglePlaceSearchService {

	GooglePlacesApiResponse search(String text, GooglePlaceType type, Rectangle box);

	GooglePlacesApiResponse searchNearby(List<String> types, Rectangle box);

	GooglePlacesApiResponse search(String text, Set<Rectangle> boxes);

	String searchPlaceId(String text);
}
