package com.groom.marky.controller;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.groom.marky.domain.response.CafeDescriptionBuilder;
import com.groom.marky.domain.response.RestaurantDescriptionBuilder;
import com.groom.marky.domain.response.ActivityDescriptionBuilder;
import com.groom.marky.service.impl.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.groom.marky.common.constant.GooglePlaceType;
import com.groom.marky.domain.request.Rectangle;
import com.groom.marky.domain.response.GooglePlacesApiResponse;
import com.groom.marky.domain.response.ParkingLotDescriptionBuilder;
import com.groom.marky.domain.response.RestaurantDescriptionBuilder;
import com.groom.marky.service.impl.EmbeddingService;
import com.groom.marky.service.impl.RedisService;
import com.groom.marky.service.impl.SeoulPlaceSearchService;
import com.groom.marky.service.impl.GooglePlaceSearchServiceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Controller
@RequestMapping("/api")
public class GoogleMapController {

    public static final String PARKING_LOT_KEYWORD = "주차장";
    public static final String CAFE_KEYWORD = "카페";
    public static final String RESTAURANT_KEYWORD = "음식점";
    private final GooglePlaceSearchServiceImpl googlePlaceSearchService;
    private final SeoulPlaceSearchService seoulPlaceSearchService;
    private final EmbeddingService embeddingService;
    private final ParkingLotDescriptionBuilder parkingLotDescriptionBuilder;
    private final RestaurantDescriptionBuilder restaurantDescriptionBuilder;
    private final CafeDescriptionBuilder cafeDescriptionBuilder;
    private final RedisService redisService;
	private final ActivityEmbeddingService activityEmbeddingService;
	private final ActivityDescriptionBuilder activityDescriptionBuilder;

    @Autowired
    public GoogleMapController(
            GooglePlaceSearchServiceImpl googlePlaceSearchService
            , SeoulPlaceSearchService seoulPlaceSearchService
            , EmbeddingService embeddingService
            , ActivityEmbeddingService activityEmbeddingService
            , ParkingLotDescriptionBuilder parkingLotDescriptionBuilder
            , RestaurantDescriptionBuilder restaruantDescriptionBuilder
            , ActivityDescriptionBuilder activityDescriptionBuilder
            , CafeDescriptionBuilder cafeDescriptionBuilder
            , RedisService redisService) {
        this.googlePlaceSearchService = googlePlaceSearchService;
        this.seoulPlaceSearchService = seoulPlaceSearchService;
        this.embeddingService = embeddingService;
        this.activityEmbeddingService = activityEmbeddingService;
        this.parkingLotDescriptionBuilder = parkingLotDescriptionBuilder;
        this.activityDescriptionBuilder = activityDescriptionBuilder;
        this.restaurantDescriptionBuilder = restaruantDescriptionBuilder;
        this.cafeDescriptionBuilder = cafeDescriptionBuilder;
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

    @GetMapping("/load/cafe")
    public ResponseEntity<?> embeddingCafe() {
        boolean sampling = //true; // 샘플링
                false; // 서울 전체
        if(sampling) {
            // kakao cafe 57
            double lat1 = 37.55855401875;
            double lon1 = 126.827750375;
            double lat2 = 37.5604405125;
            double lon2 = 126.83109553125;
            Rectangle box = new Rectangle(lon1, lat1, lon2, lat2);

            log.info("카페 탐색: ({}, {}) ~ ({}, {})",
                    box.getLow().getLatitude(),
                    box.getLow().getLongitude(),
                    box.getHigh().getLatitude(),
                    box.getHigh().getLongitude());

            GooglePlacesApiResponse response =
                    googlePlaceSearchService.search(CAFE_KEYWORD, GooglePlaceType.CAFE, box);
            log.info("구글 장소 검색 완료");
            embeddingService.saveEmbeddings(response, cafeDescriptionBuilder);
            log.info("임베딩 완료");
            redisService.setPlacesLocation(GooglePlaceType.CAFE, response);
            log.info("좌표값 레디스 저장");
        }
        else {
            Set<Rectangle> cafeBoxes = seoulPlaceSearchService.getCafeRects();
            Iterator<Rectangle> iterator = cafeBoxes.iterator();

//            Map<Rectangle, Integer> cafeBoxes = seoulPlaceSearchService.getCafeRectsMap();
//            Iterator<Map.Entry<Rectangle, Integer>> iterator = cafeBoxes.entrySet().iterator();

            int boxQty = cafeBoxes.size();
            int completeBox = 0;

            int maxRetries = 5;
            int retryCount = 1;

            while (iterator.hasNext()) {
//                Map.Entry<Rectangle, Integer> entry = iterator.next();
//                Rectangle box = entry.getKey();
//                Integer count = entry.getValue();
                Rectangle box = iterator.next();


                log.info("");
                log.info("카페 탐색: ({}, {}) ~ ({}, {})",
                        box.getLow().getLatitude(),
                        box.getLow().getLongitude(),
                        box.getHigh().getLatitude(),
                        box.getHigh().getLongitude());
                while(retryCount <= maxRetries) {
                    try {
                        GooglePlacesApiResponse response =
                                googlePlaceSearchService.search(CAFE_KEYWORD, GooglePlaceType.CAFE, box);
                        log.info("구글 장소 검색 완료");

                        embeddingService.saveEmbeddings(response, cafeDescriptionBuilder);
                        log.info("임베딩 완료");
                        redisService.setPlacesLocation(GooglePlaceType.CAFE, response);
                        log.info("좌표값 레디스 저장");

                        break;
                    } catch (HttpClientErrorException.TooManyRequests e) {
                        log.warn("429 Too Many Requests 발생 - {}번째 재시도 예정", ++retryCount);

                        try {
                            Thread.sleep(10000L * retryCount);

                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("재시도 대기 중 인터럽트 발생");
                        }
                    } catch (DataIntegrityViolationException dive){
                        log.warn("Data Integrity Violation 발생");
                        break;
                    }
                }retryCount = 1;

                log.info("{} 지역 중 {} 개 완료", boxQty, ++completeBox);
                iterator.remove();
            }
        }
        log.info("전체 카페 임베딩 완료");

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
		GooglePlacesApiResponse response = seoulPlaceSearchService.getActivityRects(keyword);
		activityEmbeddingService.saveActivityEmbeddings(response, activityDescriptionBuilder, keyword);
		redisService.setPlacesLocation(GooglePlaceType.ACTIVITY, response);

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping("/load/restaurant")
	public ResponseEntity<?> searchTextRestaurant() {

		Set<Rectangle> restaurantRects;

		try {
			// Redis에 저장된 rect가 있다면 불러오고, 없다면 새로 생성 후 저장
			restaurantRects = redisService.loadAllRects();

			if (restaurantRects.isEmpty()) {
				restaurantRects = seoulPlaceSearchService.getRestaurantRects();
				redisService.saveAllRects(restaurantRects);
				log.info("신규 격자 생성 및 Redis 저장 완료, 총 {}개", restaurantRects.size());
			} else {
				log.info("Redis에서 격자 불러옴, 총 {}개", restaurantRects.size());
			}
		} catch (JsonProcessingException e) {
			log.error("Rect JSON 파싱 오류 발생", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}

		for (Rectangle rect : restaurantRects) {
			GooglePlacesApiResponse response =
				googlePlaceSearchService.search(RESTAURANT_KEYWORD, GooglePlaceType.RESTAURANT, rect);

			int placeCount = response.places().size();

			if (placeCount == 0) {
				redisService.markRectangleAsProcessed(rect);
				continue;
			}


			embeddingService.saveRestaurantEmbeddings(response, restaurantDescriptionBuilder);
			redisService.setPlacesLocation(GooglePlaceType.RESTAURANT, response);
			redisService.markRectangleAsProcessed(rect);
		}

		log.info("임베딩 완료");

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
