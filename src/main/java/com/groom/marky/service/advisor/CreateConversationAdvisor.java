package com.groom.marky.service.advisor;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.SystemMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateConversationAdvisor implements CallAdvisor {

	private static final Pattern CONVERSATION_ID_PATTERN = Pattern.compile("conversationId\\s*:\\s*(\\S+)");

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
		log.info("[CreateConversationAdvisor] 진입");

		SystemMessage systemMessage = request.prompt().getSystemMessage();
		log.info("system : {}", systemMessage);

		if (systemMessage != null) {
			Matcher matcher = CONVERSATION_ID_PATTERN.matcher(systemMessage.getText());
			if (matcher.find()) {
				String conversationId = matcher.group(1);
				log.info("→ 추출된 conversationId: {}", conversationId);

				Map<String, Object> newContext = new HashMap<>(request.context());
				newContext.put("conversationId", conversationId);

				ChatClientRequest newRequest = request.mutate()
					.context(newContext)
					.build();

				return chain.nextCall(newRequest);
			}
		}

		// conversationId가 없으면 그대로 진행
		return chain.nextCall(request);
	}

	@Override
	public String getName() {
		return "CreateConversationAdvisor";
	}

	@Override
	public int getOrder() {
		return 0;
	}
}
