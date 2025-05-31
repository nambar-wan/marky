package com.groom.marky.domain.response;


import com.groom.marky.common.constant.GooglePlaceType;
import org.springframework.stereotype.Component;

@Component
public class RestaurantDescriptionBuilder implements DescriptionBuilder{

    @Override
    public String buildDescription(GooglePlacesApiResponse.Place place) {
        StringBuilder sb = new StringBuilder();

		if(place.userRatingCount() == 0) {
			sb.append(place.displayName().text()).append("은(는) ")
				.append(place.formattedAddress()).append("에 위치한 장소로, ")
				.append("평점과 리뷰가 존재하지 않습니다.\n\n");
		}
		else {
			sb.append(place.displayName().text()).append("은(는) ")
				.append(place.formattedAddress()).append("에 위치한 장소로, ")
				.append("평점은 ").append(place.rating()).append("점이며, 총 ")
				.append(place.userRatingCount()).append("개의 리뷰가 있습니다.\n\n");
		}
		sb.append("[식당 종류]\n");
		if(place.primaryTypeDisplayName() == null || place.primaryTypeDisplayName().text() == null) {
			sb.append("- ").append("음식점").append("\n");
		}
		else sb.append("- ").append(place.primaryTypeDisplayName().text()).append("\n");

		sb.append("\n[이용 정보]\n");
		sb.append("- 점심 제공 여부: ").append(tf(place.servesLunch())).append("\n");
		sb.append("- 저녁 제공 여부: ").append(tf(place.servesDinner())).append("\n");
		sb.append("- 브런치 제공 여부: ").append(tf(place.servesBrunch())).append("\n");
		sb.append("- 디저트 제공 여부: ").append(tf(place.servesDessert())).append("\n");
		sb.append("- 커피 제공 여부: ").append(tf(place.servesCoffee())).append("\n");
		sb.append("- 와인 제공 여부: ").append(tf(place.servesWine())).append("\n");
		sb.append("- 맥주 제공 여부: ").append(tf(place.servesBeer())).append("\n");
		sb.append("- 채식 옵션 제공 여부: ").append(tf(place.servesVegetarianFood())).append("\n");

		sb.append("\n[편의 기능]\n");
		sb.append("- 테이크아웃 가능: ").append(tf(place.takeout())).append("\n");
		sb.append("- 배달 가능: ").append(tf(place.delivery())).append("\n");
		sb.append("- 매장 내 식사 가능: ").append(tf(place.dineIn())).append("\n");
		sb.append("- 예약 가능: ").append(tf(place.reservable())).append("\n");
		sb.append("- 야외 좌석 유무: ").append(tf(place.outdoorSeating())).append("\n");
		sb.append("- 화장실 유무: ").append(tf(place.restroom())).append("\n");

		sb.append("\n[대상 고객]\n");
		sb.append("- 반려동물 동반 여부: ").append(tf(place.allowsDogs())).append("\n");
		sb.append("- 어린이 적합 여부: ").append(tf(place.goodForChildren())).append("\n");
		sb.append("- 단체 적합 여부: ").append(tf(place.goodForGroups())).append("\n");
		sb.append("- 스포츠 관람 적합 여부: ").append(tf(place.goodForWatchingSports())).append("\n");
		sb.append("- 라이브 음악 제공 여부: ").append(tf(place.liveMusic())).append("\n");
		sb.append("- 어린이 메뉴 제공 여부: ").append(tf(place.menuForChildren())).append("\n");

		if (place.reviews() != null && !place.reviews().isEmpty()) {
			sb.append("\n[리뷰]\n");
			for (GooglePlacesApiResponse.Place.Review review : place.reviews()) {
				if (review != null && review.text() != null && review.text().text() != null) {
					sb.append("- ").append(review.text().text().replaceAll("\u0000", "")
						.replaceAll("\n", " ")).append("\n");
				}
			}
		}

		return sb.toString().trim();
	}

	@Override
	public String getType() {
		return GooglePlaceType.RESTAURANT.getGoogleType();
	}


	private static String tf(Boolean value) {
		if (value == null) return "정보 없음";
		return value ? "가능" : "불가";
	}
}
