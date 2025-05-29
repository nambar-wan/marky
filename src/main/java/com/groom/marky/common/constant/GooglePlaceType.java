package com.groom.marky.common.constant;

public enum GooglePlaceType {
	CAR_DEALER("car_dealer", "자동차 판매점"),
	CAR_RENTAL("car_rental", "렌터카"),
	CAR_REPAIR("car_repair", "자동차 정비소"),
	CAR_WASH("car_wash", "세차장"),
	ELECTRIC_VEHICLE_CHARGING_STATION("electric_vehicle_charging_station", "전기차 충전소"),
	GAS_STATION("gas_station", "주유소"),
	PARKING("parking", "주차장"),
	REST_STOP("rest_stop", "휴게소"),
	ART_GALLERY("art_gallery", "미술관"),
	MUSEUM("museum", "박물관"),
	LIBRARY("library", "도서관"),
	PRESCHOOL("preschool", "유치원"),
	PRIMARY_SCHOOL("primary_school", "초등학교"),
	SECONDARY_SCHOOL("secondary_school", "중고등학교"),
	UNIVERSITY("university", "대학교"),
	AMUSEMENT_PARK("amusement_park", "놀이공원"),
	AQUARIUM("aquarium", "수족관"),
	BAR("bar", "바"),
	BOTANICAL_GARDEN("botanical_garden", "식물원"),
	BOWLING_ALLEY("bowling_alley", "볼링장"),
	CASINO("casino", "카지노"),
	COMMUNITY_CENTER("community_center", "커뮤니티 센터"),
	DOG_PARK("dog_park", "애견 공원"),
	EVENT_VENUE("event_venue", "행사장"),
	MOVIE_THEATER("movie_theater", "영화관"),
	NATIONAL_PARK("national_park", "국립공원"),
	NIGHT_CLUB("night_club", "나이트클럽"),
	PARK("park", "공원"),
	PLANETARIUM("planetarium", "천문관"),
	TOURIST_ATTRACTION("tourist_attraction", "관광 명소"),
	ZOO("zoo", "동물원"),
	ATM("atm", "ATM"),
	BANK("bank", "은행"),
	BAKERY("bakery", "빵집"),
	CAFE("cafe", "카페"),
	COFFEE_SHOP("coffee_shop", "커피숍"),
	RESTAURANT("restaurant", "레스토랑"),
	MEAL_DELIVERY("meal_delivery", "음식 배달"),
	MEAL_TAKEAWAY("meal_takeaway", "포장 전문점"),
	SUPERMARKET("supermarket", "슈퍼마켓"),
	CONVENIENCE_STORE("convenience_store", "편의점"),
	DEPARTMENT_STORE("department_store", "백화점"),
	CLOTHING_STORE("clothing_store", "의류 매장"),
	GROCERY_STORE("grocery_store", "식료품점"),
	SHOPPING_MALL("shopping_mall", "쇼핑몰"),
	ARENA("arena", "체육 경기장"),
	FITNESS_CENTER("fitness_center", "피트니스 센터"),
	GYM("gym", "헬스장"),
	PLAYGROUND("playground", "놀이터"),
	SKI_RESORT("ski_resort", "스키장"),
	STADIUM("stadium", "경기장"),
	SWIMMING_POOL("swimming_pool", "수영장"),
	AIRPORT("airport", "공항"),
	BUS_STATION("bus_station", "버스 터미널"),
	BUS_STOP("bus_stop", "버스 정류장"),
	SUBWAY_STATION("subway_station", "지하철역"),
	TAXI_STAND("taxi_stand", "택시 승강장"),
	TRAIN_STATION("train_station", "기차역"),
	CITY_HALL("city_hall", "시청"),
	COURTHOUSE("courthouse", "법원"),
	EMBASSY("embassy", "대사관"),
	FIRE_STATION("fire_station", "소방서"),
	POLICE("police", "경찰서"),
	POST_OFFICE("post_office", "우체국"),
	HOSPITAL("hospital", "병원"),
	PHARMACY("pharmacy", "약국"),
	DOCTOR("doctor", "병원(의사)"),
	ACTIVITY("activity", "액티비티");

	private final String googleType;
	private final String description;

	GooglePlaceType(String googleType, String description) {
		this.googleType = googleType;
		this.description = description;
	}

	public String getGoogleType() {
		return googleType;
	}

	public String getDescription() {
		return description;
	}
}
