package com.groom.marky.domain.response;

import static com.groom.marky.domain.response.GooglePlacesApiResponse.*;

public interface DescriptionBuilder {

	String buildDescription(Place place);

	String getType();
}
