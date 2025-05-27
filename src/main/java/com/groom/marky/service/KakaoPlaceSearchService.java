package com.groom.marky.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.groom.marky.domain.request.Rectangle;
import com.groom.marky.common.constant.KakaoMapCategoryGroupCode;
import com.groom.marky.domain.response.GooglePlacesApiResponse;

public interface KakaoPlaceSearchService {

	int MAX_PAGEABLE_COUNT = 675;

	// 사각형 내에 존재하는 특정 카테고리 토탈 카운트
	int getTotalCount(String rect, KakaoMapCategoryGroupCode code);

	int getTotalCount(String rect, String keyword);

	// 카테고리 검색
	Map<String, String> search(String rect, KakaoMapCategoryGroupCode code);

	// 범위 내 특정 카테고리를 가진 모든 장소 반환
	Map<String, String> searchAll(List<Rectangle> boxes, KakaoMapCategoryGroupCode code);

	// 키워드 검색
	Map<String, String> search(String rect, String keyword);

	Set<Rectangle> getRects(List<Rectangle> boxes, KakaoMapCategoryGroupCode categoryGroupCode);

	List<GooglePlacesApiResponse.Place> getRects(List<Rectangle> boxes, String keyword);
}
