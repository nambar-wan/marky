package com.groom.marky.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.stereotype.Component;

import com.groom.marky.service.advisor.ActivityDetailAdvisor;
import com.groom.marky.service.advisor.LocationResolverAdvisor;
import com.groom.marky.service.advisor.MultiPurposeActionAdvisor;
import com.groom.marky.service.advisor.SystemRoleAdvisor;
import com.groom.marky.service.advisor.UserIntentAdvisor;
import com.groom.marky.service.tool.ActivitySearchTool;
import com.groom.marky.service.tool.ParkingLotSearchTool;
import com.groom.marky.service.tool.RestaurantSearchTool;
import com.groom.marky.service.tool.SimilaritySearchTool;
import com.groom.marky.service.tool.SubwayRouteSearchTool;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatClientFactory {

	private final ChatModel model;
	private final ChatMemory chatMemory;
	private final SystemRoleAdvisor systemRoleAdvisor;
	private final UserIntentAdvisor userIntentAdvisor;
	private final LocationResolverAdvisor locationResolverAdvisor;
	private final ActivityDetailAdvisor activityDetailAdvisor;
	private final ParkingLotSearchTool redisGeoSearchTool;
	private final SubwayRouteSearchTool subwayRouteSearchTool;
	private final SimilaritySearchTool placeVectorSearchTool;
	private final ActivitySearchTool activitySearchTool;
	private final RestaurantSearchTool restaurantSearchTool;
	private final MultiPurposeActionAdvisor multiPurposeActionAdvisor;

	public ChatClient create(String conversationId) {

		ToolCallingChatOptions chatOptions = ToolCallingChatOptions.builder()
			.toolCallbacks(ToolCallbacks.from(
				redisGeoSearchTool,
				placeVectorSearchTool,
				activitySearchTool,
				subwayRouteSearchTool,
				restaurantSearchTool))
			.internalToolExecutionEnabled(true)
			.build();

		return ChatClient.builder(model)
			.defaultOptions(chatOptions)
			.defaultAdvisors(List.of(
				MessageChatMemoryAdvisor.builder(chatMemory)
					.conversationId(conversationId)
					.build(),
				systemRoleAdvisor,
				userIntentAdvisor,
				locationResolverAdvisor,
				multiPurposeActionAdvisor,
				activityDetailAdvisor
			))
			.build();
	}
}
