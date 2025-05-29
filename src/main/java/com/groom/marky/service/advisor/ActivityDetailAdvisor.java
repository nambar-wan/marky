package com.groom.marky.service.advisor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.marky.common.TmapGeocodingClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Slf4j
@Description("사용자 요청에서 activityDetail을 추출하는 어드바이저")
public class ActivityDetailAdvisor implements CallAdvisor {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    private static final Set<String> SUPPORTED_ACTIVITIES = Set.of(
            "클라이밍", "스크린 야구", "스크린 골프", "보드게임카페", "만화카페",
            "방탈출", "VR체험관", "PC방", "볼링장", "당구장", "아쿠아리움", "찜질방", "시장"
    );

    private static final String ACTIVITY_DETAIL = "activity_detail";

    @Autowired
    public ActivityDetailAdvisor(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {

        log.info("[ActivityDetailAdvisor] 진입");

        Map<String, Object> originalContext = new HashMap<>(request.context());
        String intent = (String) originalContext.get("intent");
        log.info("[originalContext] : {} ", originalContext);
        // 프롬프트 상세히 쓰기..
        // 지피티한테 프롬프트 짜달라하기
        // 개선된 SystemMessage 프롬프트 (프롬프트만으로 해결)

        Prompt prompt = new Prompt(List.of(
                new SystemMessage("""
                너는 intent 문장을 받아서 사용자 의도를 13개 활동 카테고리 중 하나로 정규화하는 역할만 수행한다.
                아래 명세에 따라 JSON 객체를 생성해줘. **반드시 아래 조건을 지켜라.**
                - 코드 블록이나 마크다운(```json 등)을 절대 사용하지 마.
                [출력 형식]
                {
                  "activity_detail": "스크린 야구"  // ← 값 예시
                }
                        
                [카테고리 목록 및 매핑 규칙]
                다음 13개 중 **가장 적절한 하나를 선택해서** "activity_detail"의 값으로 넣어라.
                (절대 다른 값을 추가하거나 설명하지 마라.)
                 1. "클라이밍" - 암벽등반, 볼더링, 클라이밍짐 관련 모든 표현 
                 2. "스크린 야구" - 야구연습, 배팅센터, 스크린베이스볼 관련 모든 표현 
                 3. "스크린 골프" - 골프연습장, 골프 관련 모든 표현 
                 4. "보드게임카페" - 보드게임, 보드카페, 테이블게임 관련 모든 표현 
                 5. "만화카페" - 만화방, 웹툰방, 코믹카페, 만화책 관련 모든 표현 
                 6. "방탈출" - 이스케이프룸, 방탈출게임 관련 모든 표현 
                 7. "VR체험관" - 가상현실, VR게임, VR카페 관련 모든 표현 
                 8. "PC방" - 피시방, 겜방, 게임방, 인터넷카페, 넷카페, 게임 관련 모든 표현 
                 9. "볼링장" - 볼링, 볼링센터 관련 모든 표현 
                 10. "당구장" - 당구, 포켓볼, 빌리어드, 3쿠션 관련 모든 표현 
                 11. "아쿠아리움" - 수족관, 해양박물관 관련 모든 표현 
                 12. "찜질방" - 사우나, 스파, 목욕탕, 온천 관련 모든 표현 
                 13. "시장" - 전통시장, 재래시장, 마켓, 장터 관련 모든 표현 
                  절대 위 13개 외의 다른 값을 사용하지 마라.
                  키는 모두 포함되어야 하며, 값이 없을 경우 "null"로 작성해.
                    설명은 포함하지 마. 너는 툴 콜링을 사용하면 안돼. """),
                new UserMessage(intent)
        ));

        String json = chatModel.call(prompt).getResult().getOutput().getText();
        if (!json.trim().startsWith("{")) {
            log.warn("LLM 응답이 JSON이 아님: {}", json);
            return chain.nextCall(request);
        }

        try {
            Map<String, String> extracted = objectMapper.readValue(json, new TypeReference<>() {});
            String activityDetail = extracted.get(ACTIVITY_DETAIL);

            if(!SUPPORTED_ACTIVITIES.contains(activityDetail) || activityDetail == null){
                log.info("[ActivityDetailAdvisor] 지원하지 않는 액티비티 또는 intent 없음: {} - 건너뛰기", activityDetail);
                return chain.nextCall(request);
            }
            log.info("[ActivityDetailAdvisor] 액티비티 상세 : {} ", activityDetail);

            originalContext.put(ACTIVITY_DETAIL, activityDetail);

            ChatClientRequest modified = request.mutate()
                    .context(originalContext)
                    .build();
            log.info("modified : {}", modified);

            return chain.nextCall(modified);

        } catch (Exception e) {
            log.warn("JSON 파싱 실패: {} LLM 응답: {}", e.getMessage(), json, e);
            return chain.nextCall(request);
        }
    }

    @Override
    public String getName() {
        return "UserIntentAdvisor";
    }

    @Override
    public int getOrder() {
        return 3;
    }
}


