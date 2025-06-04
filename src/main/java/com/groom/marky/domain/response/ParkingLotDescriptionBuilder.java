package com.groom.marky.domain.response;

import static com.groom.marky.domain.response.GooglePlacesApiResponse.*;

import org.springframework.stereotype.Component;

import com.groom.marky.common.constant.GooglePlaceType;

@Component
public class ParkingLotDescriptionBuilder implements DescriptionBuilder {

	@Override
	public String buildDescription(Place place) {
		StringBuilder sb = new StringBuilder();

		// 장소 기본 설명
		sb.append(place.displayName().text())
			.append("은(는) 서울특별시 ")
			.append(place.formattedAddress())
			.append("에 위치한 주차장입니다. ");

		double rating = place.rating();
		int reviewCount = place.userRatingCount();

		if (reviewCount > 0) {
			sb.append("이 장소는 총 ").append(reviewCount).append("개의 리뷰가 있으며, ")
				.append("평균 평점은 ").append(rating).append("점입니다. ");

			if (rating >= 4.5) {
				sb.append("대부분의 이용자가 매우 만족한 것으로 보이며, ")
					.append("주차 환경이 쾌적하고 접근성이 우수하다는 평가가 많습니다. ");
			} else if (rating >= 4.0) {
				sb.append("이용자들로부터 좋은 평가를 받고 있으며, ")
					.append("편리한 위치나 깔끔한 환경이 강점으로 언급되고 있습니다. ");
			} else if (rating >= 3.0) {
				sb.append("보통 수준의 평가를 받고 있으며, 일부 이용자는 만족하지만 개선이 필요한 부분도 있습니다. ");
			} else {
				sb.append("평점이 낮은 편이며, 이용 전 주의가 필요할 수 있습니다. ");
			}
		} else {
			sb.append("아직 등록된 리뷰가 없어 사용자 만족도를 판단하긴 어렵습니다. ");
		}

		// 결제 수단 정보
		if (place.paymentOptions() != null &&
			(place.paymentOptions().acceptsCreditCards() != null ||
				place.paymentOptions().acceptsDebitCards() != null ||
				place.paymentOptions().acceptsCashOnly() != null)) {

			sb.append("\n\n[결제 수단 정보]\n");

			if (place.paymentOptions().acceptsCreditCards() != null) {
				sb.append("- 신용카드 결제: ").append(tf(place.paymentOptions().acceptsCreditCards())).append("\n");
			}
			if (place.paymentOptions().acceptsDebitCards() != null) {
				sb.append("- 직불카드 결제: ").append(tf(place.paymentOptions().acceptsDebitCards())).append("\n");
			}
			if (place.paymentOptions().acceptsCashOnly() != null) {
				sb.append("- 현금만 가능 여부: ").append(tf(place.paymentOptions().acceptsCashOnly())).append("\n");
			}
		}

		// 리뷰 내용 요약
		sb.append("\n[리뷰 내용 요약]\n");
		if (place.reviews() != null && !place.reviews().isEmpty()) {
			int count = 0;
			for (Place.Review review : place.reviews()) {
				if (review != null && review.text() != null && review.text().text() != null) {
					if (count++ >= 3) break;
					sb.append("- ").append(review.text().text().replaceAll("\n", " ")).append("\n");
				}
			}
		} else {
			sb.append("등록된 리뷰가 없습니다.\n");
		}

		return sb.toString().trim();
	}

	@Override
	public String getType() {
		return GooglePlaceType.PARKING.getGoogleType();
	}

	private static String tf(Boolean value) {
		if (value == null) return "정보 없음";
		return value ? "가능" : "불가";
	}
}
