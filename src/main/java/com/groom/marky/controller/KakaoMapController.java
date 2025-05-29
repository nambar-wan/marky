package com.groom.marky.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.groom.marky.domain.request.Rectangle;
import com.groom.marky.service.impl.SeoulPlaceSearchService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/api/kakao")
public class KakaoMapController {

	private final SeoulPlaceSearchService seoulPlaceSearchService;

	@Autowired
	public KakaoMapController(SeoulPlaceSearchService seoulPlaceSearchService) {
		this.seoulPlaceSearchService = seoulPlaceSearchService;
	}

	@GetMapping("/parkinglot")
	public ResponseEntity<?> searchParkingLots() {
		// 박스 받아서 박스 범위 내의 주차장 조회 - 2000건
		Set<Rectangle> parkingLotRects = seoulPlaceSearchService.getParkingLotRects();
		log.info("parkingLotBoxes {}", parkingLotRects.size());

		return new ResponseEntity<>(parkingLotRects, HttpStatus.OK);
	}

	@GetMapping("/restaurant")
	public ResponseEntity<?> searchRestaurants() {

		Set<Rectangle> restaurantBoxes = seoulPlaceSearchService.getRestaurantRects();
		log.info("restaurantBoxes {}", restaurantBoxes.size());

		return new ResponseEntity<>(restaurantBoxes, HttpStatus.OK);
	}

	@GetMapping("/cafe")
	public ResponseEntity<?> searchCafes() {
		// 박스 받아서 박스 범위 내의 카페 조회
		Set<Rectangle> cafeBoxes = seoulPlaceSearchService.getCafeRects();
		log.info("cafeBoxes {}", cafeBoxes.size());

		return new ResponseEntity<>(cafeBoxes, HttpStatus.OK);
	}

	@GetMapping("/cafe/map")
	public ResponseEntity<?> searchCafeMap() {
		// 박스 범위 내의 카페 맵으로 전달
		Map<Rectangle, Integer> cafeBoxes = seoulPlaceSearchService.getCafeRectsMap();
		log.info("cafeBoxes {}", cafeBoxes.size());

		return new ResponseEntity<>(cafeBoxes, HttpStatus.OK);
	}
}
