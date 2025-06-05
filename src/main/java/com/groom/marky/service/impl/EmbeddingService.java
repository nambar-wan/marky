package com.groom.marky.service.impl;

import static com.groom.marky.common.constant.MetadataKeys.*;
import static com.groom.marky.domain.response.GooglePlacesApiResponse.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.groom.marky.domain.response.DescriptionBuilder;
import com.groom.marky.domain.response.GooglePlacesApiResponse;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmbeddingService {

	private final VectorStore vectorStore;
	private final RedisService redisService;

	@Autowired
	public EmbeddingService(VectorStore vectorStore, RedisService redisService) {
		this.vectorStore = vectorStore;
		this.redisService = redisService;
	}

	public void saveEmbeddings(GooglePlacesApiResponse apiResponse, DescriptionBuilder descriptionBuilder) {

		List<Place> places = apiResponse.places();

		// UUID 비교, 업데이트..
		List<Document> documents = places.stream()
			.map(place -> {
				String description = descriptionBuilder.buildDescription(place);
				String id = UUID.nameUUIDFromBytes(place.id().getBytes(StandardCharsets.UTF_8)).toString();

				return new Document(
					id,
					description, // 자연어
					Map.of(
						GOOGLEPLACEID, place.id(),
						DISPLAYNAME, place.displayName().text(),
						TYPE, descriptionBuilder.getType(),
						LAT, place.location().latitude(),
						LON, place.location().longitude(),
						FORMATTEDADDRESS, place.formattedAddress(),
						USERRATINGCOUNT, place.userRatingCount(),
						RATING, place.rating()
					));
			}).toList();

		vectorStore.add(documents);
	}

	public void saveRestaurantEmbeddings(GooglePlacesApiResponse apiResponse, DescriptionBuilder descriptionBuilder) {

		List<Place> places = apiResponse.places();

		// UUID 비교, 업데이트..
		List<Document> documents = places.stream()
			.map(place -> {
				String description = descriptionBuilder.buildDescription(place);
				String id = UUID.nameUUIDFromBytes(place.id().getBytes(StandardCharsets.UTF_8)).toString();

				log.info("placeId={}, length={}", place.id(), description.length());
				if(description.length() > 7400) {
					log.info("!!!!!!!!!!!!!!토큰초과 예상됨!!!!!!!!!!!!!!!!");
					log.info("placeId={}, length={}", place.id(), description.length());
					redisService.markPlaceAsOverLength(place);
				}

				String type = "음식점";
				if(!(place.primaryTypeDisplayName() == null || place.primaryTypeDisplayName().text() == null)) {
					type = place.primaryTypeDisplayName().text();
				}

				return new Document(
					id,
					description, // 자연어
					Map.of(
						GOOGLEPLACEID, place.id(),
						DISPLAYNAME, place.displayName().text(),
						PRIMARYTYPE, type,
						TYPE, descriptionBuilder.getType(),
						LAT, place.location().latitude(),
						LON, place.location().longitude(),
						FORMATTEDADDRESS, place.formattedAddress(),
						USERRATINGCOUNT, place.userRatingCount(),
						RATING, place.rating()
					));
			}).toList();

		vectorStore.add(documents);
	}

	public void saveParkingLotsEmbeddings(GooglePlacesApiResponse apiResponse, DescriptionBuilder descriptionBuilder) {

		List<Place> places = apiResponse.places();

		/**
		 *  "primaryTypeDisplayName": {
		 *         "text": "주차장",
		 *         "languageCode": "ko"
		 *       },
		 */
		// UUID 비교, 업데이트..
		List<Document> documents = places.stream()
			.filter(place ->
				place.primaryTypeDisplayName() != null &&
					"주차장".equals(place.primaryTypeDisplayName().text())
			)
			.map(place -> {
				String description = descriptionBuilder.buildDescription(place);
				String id = UUID.nameUUIDFromBytes(place.id().getBytes(StandardCharsets.UTF_8)).toString();

				return new Document(
					id,
					description, // 자연어
					Map.of(
						GOOGLEPLACEID, place.id(),
						DISPLAYNAME, place.displayName().text(),
						TYPE, descriptionBuilder.getType(),
						LAT, place.location().latitude(),
						LON, place.location().longitude(),
						FORMATTEDADDRESS, place.formattedAddress(),
						USERRATINGCOUNT, place.userRatingCount(),
						RATING, place.rating()
					));
			}).toList();

		vectorStore.add(documents);
	}

}
