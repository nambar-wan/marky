package com.groom.marky.domain.request;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
/**
 * 기본 생성자가 있다면, 빌더는 해당 생성자를 사용한다.
 * @NoArgsConstructor(access = AccessLevel.PROTECTED) 만 두게 되면, 필드 값들을 넣을 생성자가 없어서 에러가 발생한다.
 */
@Builder
@AllArgsConstructor
public class PlacesNearbyRequest {
	private List<String> includedTypes;
	private String languageCode;
	private String regionCode;
	private LocationRestriction locationRestriction;


}


