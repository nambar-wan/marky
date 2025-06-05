package com.groom.marky.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class ChatLog extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "conversation_id") // "conversation_id" 컬럼 생성
	private Conversation conversation;

	// @Lob
	@Column(columnDefinition = "text")
	private String question;

	//@Lob
	@Column(columnDefinition = "text")
	private String answer;

	private int inputToken;

	private int outputToken;

	private int totalToken;

	public void setConversation(Conversation conversation) {
		this.conversation = conversation;
	}
}
