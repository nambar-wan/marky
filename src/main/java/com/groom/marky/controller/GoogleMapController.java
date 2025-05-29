package com.groom.marky.controller;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.groom.marky.domain.response.CafeDescriptionBuilder;
import com.groom.marky.domain.response.RestaruantDescriptionBuilder;
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
    public static final String CAFE_KEYWORD = "카페";
    private final GooglePlaceSearchServiceImpl googlePlaceSearchService;
    private final SeoulPlaceSearchService seoulPlaceSearchService;
    private final EmbeddingService embeddingService;
    private final ParkingLotDescriptionBuilder parkingLotDescriptionBuilder;
    private final RestaruantDescriptionBuilder restaruantDescriptionBuilder;
    private final CafeDescriptionBuilder cafeDescriptionBuilder;
    private final RedisService redisService;

    @Autowired
    public GoogleMapController(GooglePlaceSearchServiceImpl googlePlaceSearchService, SeoulPlaceSearchService seoulPlaceSearchService, EmbeddingService embeddingService, ParkingLotDescriptionBuilder parkingLotDescriptionBuilder, RestaruantDescriptionBuilder restaruantDescriptionBuilder, CafeDescriptionBuilder cafeDescriptionBuilder, RedisService redisService) {
        this.googlePlaceSearchService = googlePlaceSearchService;
        this.seoulPlaceSearchService = seoulPlaceSearchService;
        this.embeddingService = embeddingService;
        this.parkingLotDescriptionBuilder = parkingLotDescriptionBuilder;
        this.restaruantDescriptionBuilder = restaruantDescriptionBuilder;
        this.cafeDescriptionBuilder = cafeDescriptionBuilder;
        this.redisService = redisService;
    }

    @GetMapping("/load/parkinglot")
    public ResponseEntity<?> embeddingParkingLot() {

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
        boolean sampling = true; // 샘플링
                            //false; // 서울 전체
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

            while (iterator.hasNext()) {
                Rectangle box = iterator.next();

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

                iterator.remove();
            }
        }
        log.info("카페 임베딩 완료");

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
}
