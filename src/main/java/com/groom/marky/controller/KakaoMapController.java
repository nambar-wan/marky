package com.groom.marky.controller;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.groom.marky.domain.request.Rectangle;
import com.groom.marky.service.KakaoPlaceSearchService;
import com.groom.marky.service.impl.KakaoPlaceSearchServiceImpl;
import com.groom.marky.service.impl.SeoulPlaceSearchService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/api/kakao")
public class KakaoMapController {

	private final SeoulPlaceSearchService seoulPlaceSearchService;
	private final KakaoPlaceSearchService kakaoPlaceSearchService;

	@Autowired
	public KakaoMapController(SeoulPlaceSearchService seoulPlaceSearchService,
		KakaoPlaceSearchService kakaoPlaceSearchService) {
		this.seoulPlaceSearchService = seoulPlaceSearchService;
		this.kakaoPlaceSearchService = kakaoPlaceSearchService;
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
	public ResponseEntity<?> getCafes() {
		Set<Rectangle> cafeBoxes = seoulPlaceSearchService.getCafeRects();
		log.info("cafeBoxes {}", cafeBoxes.size());
		return new ResponseEntity<>(cafeBoxes, HttpStatus.OK);
	}


	@GetMapping("/search")
	public ResponseEntity<?> searchKeyword(@RequestParam("keyword") String keyword) {


		Map<String, Double> search = kakaoPlaceSearchService.search(keyword);

		log.info("search result : {}", search);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
