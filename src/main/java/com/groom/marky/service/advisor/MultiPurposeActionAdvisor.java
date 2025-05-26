package com.groom.marky.service.advisor;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.annotation.Description;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Description("사용자의 intent, location, mood context를 기반으로 searchParkingLots와 similaritySearch 툴 호출을 구성하고 실행을 유도하는 어드바이저")
public class MultiPurposeActionAdvisor implements CallAdvisor {

	private static final String toolHint = """
		사용할 수 있는 툴 목록:
		
		1. searchParkingLots(lat : Double, lon : Double)
		- 사용자의 현재 위치를 기준으로 주변 주차장의 고유 ID 목록을 조회합니다.
		- 반환된 ID 리스트는 이후 추천할 대상의 범위를 한정할 때 사용됩니다.
		
		2. similaritySearch(mood: String, ids: List<String>)
		- 사용자가 원하는 분위기(mood)와 의미적으로 유사한 장소를 5개 추천합니다.
		- 이 함수는 pgvector 기반의 벡터 임베딩을 사용하여 장소 설명과 mood 간의 의미 유사도를 비교합니다.
		- 'ids' 파라미터로 전달된 주차장 목록 중에서 분위기와 가장 유사한 장소를 추출합니다.
		
		
		[mood 설명]
		- mood는 사용자가 원하는 분위기나 조건을 뜻하는 텍스트로, 예를 들어 다음과 같은 값이 있습니다:
		  - 리뷰가 있는
		  - 리뷰가 좋은
		  - 평점이 높은
		  - 쾌적한
		  - 조용한
		  - 혼잡하지 않은
		  - 넓은 공간
		  - 안전한
		  - 접근성이 좋은
		- mood 값은 자연어 그대로 전달하면 됩니다. 예: mood="리뷰가 좋은"
		
		[예시]
		- '리뷰가 좋은 주차장을 추천해줘' → similaritySearch(mood="리뷰가 좋은", ids=[...])
		- '조용하고 쾌적한 주차장' → similaritySearch(mood="조용하고 쾌적한", ids=[...])
		
		요청에 맞는 툴을 위 형식대로 호출해 주세요.
		""";

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
		log.info("[MultiPurposeActionAdvisor] 진입");
		Map<String, Object> context = request.context();

		// 컨텍스트 로깅
		context.forEach((k, v) -> log.info("key : {}, value : {}", k, v));

		// context 요약 메시지 생성
		StringBuilder contextSummary = new StringBuilder("현재 사용자의 context 정보:\n");

		context.forEach((k, v) -> contextSummary.append("- ").append(k).append(": ").append(v).append("\n"));

		// 병합된 프롬프트 생성
		Prompt merged = request.prompt()
			.augmentSystemMessage(toolHint + "\n\n" + contextSummary);

		// mutate()를 사용해 기존 옵션 유지하며 프롬프트와 toolContext 설정
		ChatClientRequest updatedRequest = request.mutate()
			.prompt(merged)
			.build();

		// 체인으로 전달
		ChatClientResponse response = chain.nextCall(updatedRequest);

		// 결과 로깅
		log.info("api 응답: {}", response.chatResponse().getResult().getOutput());
		log.info("toolCalls: {}", response.chatResponse().getResult().getOutput().getToolCalls());
		log.info("metadata: {}", response.chatResponse().getResult().getOutput().getMetadata());

		return response;
	}

	@Override
	public String getName() {
		return "MultiPurposeActionAdvisor";
	}

	@Override
	public int getOrder() {
		return 3;
	}
}
