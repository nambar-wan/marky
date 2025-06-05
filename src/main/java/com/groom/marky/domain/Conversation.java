package com.groom.marky.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Conversation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, updatable = false)
	private String conversationId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id") // user_id 라는 외래 키 컬럼 생성. User 테이블의 @Id 와 참조 관계
	private User user;

	private String title;

	@OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ChatLog> chatLogs = new ArrayList<>();

	@Builder
	public Conversation(String conversationId, User user) {
		this.conversationId = conversationId;
		this.user = user;
		chatLogs = new ArrayList<>();
	}

	@PrePersist
	private void assignUuid() {
		if (this.conversationId == null) {
			this.conversationId = UUID.randomUUID().toString();
		}
	}

	public void addChatLog(ChatLog chatLog) {
		chatLogs.add(chatLog);
		chatLog.setConversation(this);
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
