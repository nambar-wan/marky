package com.groom.marky.domain.response;

import static com.groom.marky.domain.response.GooglePlacesApiResponse.*;

import org.springframework.stereotype.Component;

import com.groom.marky.common.constant.GooglePlaceType;

@Component
public class ActivityDescriptionBuilder implements DescriptionBuilder {

    @Override
    public String buildDescription(Place place) {
        StringBuilder sb = new StringBuilder();

        // 기본 정보
        sb.append(place.displayName().text()).append("은(는) ")
                .append(place.formattedAddress()).append("에 위치한 장소로, ")
                .append("평점은 ").append(place.rating()).append("점이며, 총 ");

        int reviewCount = place.userRatingCount();
        sb.append(reviewCount).append("개의 리뷰가 ");

        if (reviewCount > 0) {
            sb.append("있습니다. ");
            if (place.rating() >= 4.5) {
                sb.append("사용자들의 평가는 매우 좋습니다. ");
            } else if (place.rating() >= 4.0) {
                sb.append("사용자들의 평가가 좋은 편입니다. ");
            } else if (place.rating() >= 3.0) {
                sb.append("평균적인 평가를 받고 있습니다. ");
            } else {
                sb.append("평점이 낮은 편입니다. ");
            }
        } else {
            sb.append("없습니다. ");
            sb.append("리뷰가 없어 사용자 평가는 확인되지 않습니다. ");
        }

        sb.append("\n\n[이용 정보]\n");

        // 결제 수단 정보
        sb.append("\n[결제 수단]\n");
        if (place.paymentOptions() != null) {
            sb.append("- 신용카드 사용: ").append(tf(place.paymentOptions().acceptsCreditCards())).append("\n");
            sb.append("- 직불카드 사용: ").append(tf(place.paymentOptions().acceptsDebitCards())).append("\n");
            sb.append("- 현금만 결제: ").append(tf(place.paymentOptions().acceptsCashOnly())).append("\n");
        } else {
            sb.append("- 결제 수단 정보 없음\n");
        }

        sb.append("\n[편의 시설]\n");

        appendIfKnown(sb, "화장실", place.restroom());
        appendIfKnown(sb, "매장 내 식사 가능", place.dineIn());
        appendIfKnown(sb, "배달 가능", place.delivery());
        appendIfKnown(sb, "포장 가능", place.takeout());
        if (place.parkingOptions() != null) {
            sb.append("- 주차장: 있음\n");
        }

        appendIfKnown(sb, "단체 이용 적합", place.goodForGroups());
        appendIfKnown(sb, "어린이 동반 적합", place.goodForChildren());
        appendIfKnown(sb, "예약 가능", place.reservable());

        // 리뷰 텍스트 요약
        if (place.reviews() != null && !place.reviews().isEmpty()) {
            sb.append("\n[리뷰]\n");
            for (Place.Review review : place.reviews()) {
                if (review != null && review.text() != null && review.text().text() != null) {
                    sb.append("- ").append(review.text().text().replaceAll("\n", " ")).append("\n");
                }
            }
        } else {
            sb.append("\n[리뷰 없음]\n");
        }

        return sb.toString().trim();
    }

    @Override
    public String getType() {
        return GooglePlaceType.ACTIVITY.getGoogleType();
    }

    private static String tf(Boolean value) {
        if (value == null)
            return "정보 없음";
        return value ? "가능" : "불가";
    }

    private static void appendIfKnown(StringBuilder sb, String label, Boolean value) {
        if (value != null) {
            sb.append("- ").append(label).append(": ").append(tf(value)).append("\n");
        }
    }
}
