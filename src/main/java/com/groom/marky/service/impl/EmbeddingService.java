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

@Service
public class EmbeddingService {


	private final VectorStore vectorStore;

	@Autowired
	public EmbeddingService(VectorStore vectorStore) {
		this.vectorStore = vectorStore;
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

}
