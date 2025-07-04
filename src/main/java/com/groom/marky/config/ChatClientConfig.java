package com.groom.marky.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.groom.marky.common.TmapGeocodingClient;
import com.groom.marky.common.TmapTransitClient;
import com.groom.marky.repository.CustomChatMemoryRepository;
import com.groom.marky.service.KakaoPlaceSearchService;
import com.groom.marky.service.advisor.ActivityDetailAdvisor;
import com.groom.marky.service.advisor.LocationResolverAdvisor;
import com.groom.marky.service.advisor.MultiPurposeActionAdvisor;
import com.groom.marky.service.advisor.SystemRoleAdvisor;
import com.groom.marky.service.advisor.UserIntentAdvisor;


@EnableJpaAuditing
@Configuration
public class ChatClientConfig {

	@Bean
	public ChatMemory chatMemory(CustomChatMemoryRepository chatMemoryRepository) {
		return MessageWindowChatMemory.builder()
			.chatMemoryRepository(chatMemoryRepository)
			.maxMessages(10)
			.build();
	}

	// @Bean
	// public ChatClient chatClient(
	// 	ChatMemory chatMemory,
	// 	ChatModel model,
	// 	SystemRoleAdvisor systemRoleAdvisor,
	// 	UserIntentAdvisor userIntentAdvisor,
	// 	LocationResolverAdvisor locationResolverAdvisor,
	// 	ActivityDetailAdvisor activityDetailAdvisor,
	// 	RedisGeoSearchTool redisGeoSearchTool,
	// 	SubwayRouteSearchTool subwayRouteSearchTool,
	// 	PlaceVectorSearchTool placeVectorSearchTool,
	// 	ActivitySearchTool activitySearchTool,
	// 	RestaurantSearchTool restaurantSearchTool,
	// 	MultiPurposeActionAdvisor multiPurposeActionAdvisor) {
	//
	// 	ToolCallingChatOptions chatOptions = ToolCallingChatOptions.builder()
	// 		.toolCallbacks(
	// 			ToolCallbacks.from(
	// 				redisGeoSearchTool, placeVectorSearchTool, activitySearchTool, subwayRouteSearchTool,
	// 				restaurantSearchTool))
	// 		.internalToolExecutionEnabled(true)
	// 		.build();
	//
	// 	return ChatClient.builder(model)
	// 		.defaultOptions(chatOptions)
	// 		.defaultAdvisors(List.of(
	// 			MessageChatMemoryAdvisor.builder(chatMemory).build(),
	// 			systemRoleAdvisor,
	// 			userIntentAdvisor,
	// 			locationResolverAdvisor,
	// 			multiPurposeActionAdvisor,
	// 			activityDetailAdvisor,
	// 			multiPurposeActionAdvisor
	// 		))
	// 		.build();
	// }

	@Bean
	public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
		return restTemplate;
	}

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		return mapper;
	}

	@Bean
	public UserIntentAdvisor userIntentAdvisor(ChatModel chatModel, ObjectMapper objectMapper) {
		return new UserIntentAdvisor(chatModel, objectMapper);
	}

	@Bean
	public SystemRoleAdvisor systemRoleAdvisor() {
		return new SystemRoleAdvisor();
	}

	@Bean
	public TmapGeocodingClient tmapGeocodingClient(RestTemplate restTemplate) {
		return new TmapGeocodingClient(restTemplate);
	}

	@Bean
	public LocationResolverAdvisor locationResolverAdvisor(KakaoPlaceSearchService kakaoPlaceSearchService, ChatModel chatModel, ObjectMapper objectMapper) {
		return new LocationResolverAdvisor(kakaoPlaceSearchService, chatModel, objectMapper);
	}

	@Bean
	public MultiPurposeActionAdvisor multiPurposeActionAdvisor() {
		return new MultiPurposeActionAdvisor();
	}

	@Bean
	public TmapTransitClient tmapTransitClient(ObjectMapper objectMapper, RestTemplate restTemplate) {
		return new TmapTransitClient(objectMapper, restTemplate);
	}

	@Bean
	public ActivityDetailAdvisor activityDetailAdvisor(ChatModel chatModel, ObjectMapper objectMapper) {
		return new ActivityDetailAdvisor(chatModel, objectMapper);
	}

}
