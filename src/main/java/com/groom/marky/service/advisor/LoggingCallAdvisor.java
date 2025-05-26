package com.groom.marky.service.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingCallAdvisor implements CallAdvisor {
	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
		long start = System.currentTimeMillis();
		try {
			return chain.nextCall(request);
		} finally {
			log.info("Elapsed: {}ms", System.currentTimeMillis() - start);
		}
	}

	@Override
	public String getName() {
		return "LoggingCallAdvisor"; // advisor 이름
	}

	@Override
	public int getOrder() {
		return 100; // 로깅은 후순위에서 실행
	}
}
