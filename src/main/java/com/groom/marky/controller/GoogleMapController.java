package com.groom.marky.controller;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.groom.marky.common.constant.GooglePlaceType;
import com.groom.marky.domain.request.Rectangle;
import com.groom.marky.domain.response.GooglePlacesApiResponse;
import com.groom.marky.domain.response.ParkingLotDescriptionBuilder;
import com.groom.marky.service.impl.EmbeddingService;
import com.groom.marky.service.impl.RedisService;
import com.groom.marky.service.impl.SeoulPlaceSearchService;
import com.groom.marky.service.impl.GooglePlaceSearchServiceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/api")
public class GoogleMapController {

	public static final String PARKING_LOT_KEYWORD = "주차장";
	private final GooglePlaceSearchServiceImpl googlePlaceSearchService;
	private final SeoulPlaceSearchService seoulPlaceSearchService;
	private final EmbeddingService embeddingService;
	private final ParkingLotDescriptionBuilder parkingLotDescriptionBuilder;
	private final RedisService redisService;

	@Autowired
	public GoogleMapController(GooglePlaceSearchServiceImpl googlePlaceSearchService,
		SeoulPlaceSearchService seoulPlaceSearchService, EmbeddingService embeddingService,
		ParkingLotDescriptionBuilder parkingLotDescriptionBuilder, RedisService redisService) {
		this.googlePlaceSearchService = googlePlaceSearchService;
		this.seoulPlaceSearchService = seoulPlaceSearchService;
		this.embeddingService = embeddingService;
		this.parkingLotDescriptionBuilder = parkingLotDescriptionBuilder;
		this.redisService = redisService;
	}

	@GetMapping("/load/parkinglot")
	public ResponseEntity<?> searchText() {

		// kakao cafe 57
		Rectangle box = new Rectangle(
			127.0016985,
			37.684949100000004,
			127.055221,
			37.715133);

	//	Set<Rectangle> parkingLotRects = seoulPlaceSearchService.getParkingLotRects();
		GooglePlacesApiResponse response = googlePlaceSearchService.search(PARKING_LOT_KEYWORD, GooglePlaceType.PARKING,
			box);

		embeddingService.saveEmbeddings(response, parkingLotDescriptionBuilder);
		redisService.setPlacesLocation(GooglePlaceType.PARKING, response);



	/*	for (Rectangle rect : parkingLotRects) {
			GooglePlacesApiResponse response =
				googlePlaceSearchService.search(PARKING_LOT_KEYWORD, GooglePlaceType.PARKING, rect);

			int placeCount = response.places().size();

			if (placeCount == 0) {
				continue;
			}


			embeddingService.saveEmbeddings(response, parkingLotDescriptionBuilder);
			redisService.setPlacesLocation(GooglePlaceType.PARKING, response);
		}*/

		log.info("임베딩 완료");

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping("/google/search/nearby")
	public ResponseEntity<?> searchNearby() {

		// 126.91844127777779,37.550798433333334,126.92141475000001,37.552475316666666, total : 42

		// kakao cafe 50
		Rectangle box = new Rectangle(
			127.0016985,
			37.684949100000004,
			127.055221,
			37.715133); // 50

		// 응답
		GooglePlacesApiResponse response = googlePlaceSearchService.searchNearby(List.of("restaurant"), box);

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping("/activity")
	public ResponseEntity<?> getActivity(@RequestParam("keyword") String keyword) {
		List<GooglePlacesApiResponse.Place> activityBoxes = seoulPlaceSearchService.getActivityRects(keyword);
		log.info("activityBoxes {}", activityBoxes.size());

		return new ResponseEntity<>(activityBoxes, HttpStatus.OK);
	}
}
