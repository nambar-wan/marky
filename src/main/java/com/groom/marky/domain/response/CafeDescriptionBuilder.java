package com.groom.marky.domain.response;

import com.groom.marky.common.constant.GooglePlaceType;
import org.springframework.stereotype.Component;

@Component
public class CafeDescriptionBuilder implements DescriptionBuilder {

    @Override
    public String buildDescription(GooglePlacesApiResponse.Place place) {
        StringBuilder sb = new StringBuilder();

        sb.append(place.displayName().text()).append("은(는) ")
                .append(place.formattedAddress()).append("에 위치한 카페로, \n");
        if(place.userRatingCount()==0){
            sb.append("아직까지 등록된 리뷰가 없습니다. \n\n");
        }
        else {
            sb.append("평점은 ").append(place.rating()).append("점이며, ")
            .append("총 ").append(place.userRatingCount()).append("개의 리뷰가 있습니다.\n");
            if(place.rating()>=4.5){sb.append("이용자들의 평가가 매우 좋습니다.\n\n");}
            else if(place.rating() >= 4.0) {sb.append("이용자들의 평가가 좋습니다.\n\n");}
            else if(place.rating() >= 3.5) {sb.append("이용자들의 평가가 양호합니다.\n\n");}
            else if(place.rating() >= 3.0) {sb.append("이용자들의 평가가 그저그렇습니다.\n\n");}
            else sb.append("이용자들의 평가가 안좋습니다.\n\n");
        }
        // 오픈시간 클로즈시간

//        sb.append("[이용 시간]\n");
//        sb.append(place.weekdayDescriptions()).append("\n\n");


        sb.append("[이용 정보]\n");
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

        sb.append("\n[주차 제공]\n");
        if(place.parkingOptions() != null) {
            sb.append("- 무료 주차장: ").append(tf(place.parkingOptions().freeParkingLot())).append("\n");
            sb.append("- 유료 주차장: ").append(tf(place.parkingOptions().paidParkingLot())).append("\n");
            sb.append("- 무료 노상 주차: ").append(tf(place.parkingOptions().freeStreetParking())).append("\n");
            sb.append("- 발레파킹: ").append(tf(place.parkingOptions().valetParking())).append("\n");
            sb.append("- 무료 실내 주차: ").append(tf(place.parkingOptions().freeGarageParking())).append("\n");
            sb.append("- 유료 실내 주차: ").append(tf(place.parkingOptions().paidGarageParking())).append("\n\n");
        }
        else sb.append("- 주차 정보 없음 \n\n");

        sb.append("\n[결제 수단]\n");
        if (place.paymentOptions() != null) {
            sb.append("- 신용카드 사용: ").append(tf(place.paymentOptions().acceptsCreditCards())).append("\n");
            sb.append("- 직불카드 사용: ").append(tf(place.paymentOptions().acceptsDebitCards())).append("\n");
            sb.append("- 현금만 결제: ").append(tf(place.paymentOptions().acceptsCashOnly())).append("\n");
        } else {
            sb.append("- 결제 수단 정보 없음\n");
        }

        if (place.reviews() != null && !place.reviews().isEmpty()) {
            sb.append("\n[리뷰]\n");
            for (GooglePlacesApiResponse.Place.Review review : place.reviews()) {
                if (review != null && review.text() != null && review.text().text() != null) {
                    sb.append("- ").append(review.text().text().replaceAll("\n", " ")).append("\n");
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
