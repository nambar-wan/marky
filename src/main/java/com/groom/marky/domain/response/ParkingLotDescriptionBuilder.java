package com.groom.marky.domain.response;

import static com.groom.marky.domain.response.GooglePlacesApiResponse.*;

import org.springframework.stereotype.Component;

import com.groom.marky.common.constant.GooglePlaceType;

@Component
public class ParkingLotDescriptionBuilder implements DescriptionBuilder {

	@Override
	public String buildDescription(Place place) {
		StringBuilder sb = new StringBuilder();

		// 기본 설명
		sb.append(place.displayName().text())
			.append("은(는) ")
			.append(place.formattedAddress())
			.append("에 위치한 주차장입니다. ");

		double rating = place.rating();
		int reviewCount = place.userRatingCount();

		sb.append("이 주차장은 총 ").append(reviewCount).append("개의 리뷰가 있으며, ")
			.append("이용자들의 평가를 바탕으로 평균 평점은 ").append(rating).append("점입니다. ");

		// 평점 기반 자연어 설명: 리뷰/평점/평가 키워드 명시 포함
		if (rating >= 4.5) {
			sb.append("리뷰 내용은 전반적으로 매우 긍정적이며, ")
				.append("평점이 높고 사용자들의 평가가 뛰어난 편입니다. ")
				.append("리뷰 점수도 우수하여, 쾌적하고 편리한 주차장을 원하는 분들에게 적합합니다. ")
				.append("리뷰가 좋은 주차장, 평가가 좋은 주차장, 쾌적한 주차장으로 추천됩니다. ");
		} else if (rating >= 4.0) {
			sb.append("리뷰 점수는 높은 편이며, 대체로 좋은 평가를 받고 있습니다. ")
				.append("위치나 편의성 측면에서 만족스러운 경험을 제공한다는 의견이 많습니다. ")
				.append("리뷰가 괜찮고 평가가 좋은 주차장을 찾는 사용자에게 적합할 수 있습니다. ");
		} else if (rating >= 3.0) {
			sb.append("평점은 보통 수준이며, 리뷰에는 긍정적인 내용과 함께 개선이 필요한 점도 언급되어 있습니다. ")
				.append("리뷰와 평점 모두 참고하여 방문을 결정하는 것이 좋습니다. ");
		} else {
			sb.append("리뷰 점수와 평점이 모두 낮은 편으로, ")
				.append("이용자들의 평가가 좋지 않다는 점을 고려할 필요가 있습니다. ")
				.append("이용 전 주차 환경과 실제 후기를 확인하는 것이 안전합니다. ");
		}

		// 결제 수단
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

		// 리뷰 요약
		sb.append("\n[리뷰 내용 요약]\n");
		int count = 0;
		for (Place.Review review : place.reviews()) {
			if (review != null && review.text() != null && review.text().text() != null) {
				if (count++ >= 5) break;
				sb.append("- ").append(review.text().text().replaceAll("\n", " ")).append("\n");
			}
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
