package com.groom.marky.domain.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	include = JsonTypeInfo.As.WRAPPER_OBJECT,
	property = ""
)
@JsonSubTypes({
	@JsonSubTypes.Type(value = Rectangle.class, name = "rectangle"),
	@JsonSubTypes.Type(value = Circle.class, name = "circle")
})
public interface LocationRestriction {

	/**
	 * 다형성 활용을 위해 인터페이스에서 구현체 별 직렬화 / 역직렬화 명시
	 *
	 * @JsonTypeInfo 를 사용, 특징 필드 값을 참조하여 자동으로 맵핑.
	 * 어떤 필드가 어떤 클래스 맵핑될지 결정.
	 *
	 * @JsonSubTypes 으로 그 하위 타입 설정.
	 *
	 *
	 *
	 */
}
