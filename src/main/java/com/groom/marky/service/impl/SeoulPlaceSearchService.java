package com.groom.marky.service.impl;

import static com.groom.marky.common.constant.KakaoMapCategoryGroupCode.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.groom.marky.domain.response.GooglePlacesApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.groom.marky.domain.request.Rectangle;
import com.groom.marky.service.KakaoPlaceSearchService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SeoulPlaceSearchService {

	private final KakaoPlaceSearchService kakaoPlaceSearchService;
	private static final Rectangle seoulBox = Rectangle.rectOfSeoul();

	// 서울을 10 X 10 으로 나눔
	private static final List<Rectangle> seoulBoxes = seoulBox.generateGrid(10, 10);

	@Autowired
	public SeoulPlaceSearchService(KakaoPlaceSearchService kakaoPlaceSearchService) {
		this.kakaoPlaceSearchService = kakaoPlaceSearchService;
	}

	public Map<String, String> collectParkingLot() {
		return kakaoPlaceSearchService.searchAll(seoulBoxes, PK6);
	}

	public Map<String, String> collectCafe() {
		return kakaoPlaceSearchService.searchAll(seoulBoxes, CE7);
	}

	public Map<String, String> collectRestaurant() {
		return kakaoPlaceSearchService.searchAll(seoulBoxes, FD6);
	}

	public Set<Rectangle> getCafeRects() {
		return kakaoPlaceSearchService.getRects(seoulBoxes, CE7);
	}

	public Map<Rectangle, Integer> getCafeRectsMap() {
		return kakaoPlaceSearchService.getRectsMap(seoulBoxes, CE7);
	}

	public Set<Rectangle> getRestaurantRects() {

		return kakaoPlaceSearchService.getRects(seoulBoxes, FD6);
	}

	public Set<Rectangle> getParkingLotRects() {

		return kakaoPlaceSearchService.getRects(seoulBoxes, PK6);
	}

	public GooglePlacesApiResponse getActivityRects(String keyword) {
		return kakaoPlaceSearchService.getRects(seoulBoxes, keyword);
	}
}

