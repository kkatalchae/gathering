package com.gathering.chat.domain.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.gathering.gathering.domain.model.GatheringEntity;
import com.gathering.schedule.domain.model.ScheduleEntity;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅방 엔티티
 * 모임 또는 일정에 하나씩 자동 생성되며, 어느 쪽인지는 roomType 으로 명시한다 (null 여부로 추론하지 않는다)
 * 채팅방 멤버 테이블은 두지 않는다 — 멤버십은 주체(모임/일정)의 참여자 테이블에서 파생된다
 * 근거: docs/adr/0002-chat-room-membership.md
 */
@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "chat_rooms",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_chat_room_gathering", columnNames = "gathering_tsid"),
		@UniqueConstraint(name = "uk_chat_room_schedule", columnNames = "schedule_tsid")
	}
)
public class ChatRoomEntity {

	@Id
	@Tsid
	@Column(nullable = false, length = 13, columnDefinition = "CHAR(13)")
	private String tsid;

	@Column(name = "room_type", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private ChatRoomType roomType;

	/** roomType 이 GATHERING 일 때만 값이 있다 */
	@Column(name = "gathering_tsid", length = 13, columnDefinition = "CHAR(13)")
	private String gatheringTsid;

	/** roomType 이 SCHEDULE 일 때만 값이 있다 */
	@Column(name = "schedule_tsid", length = 13, columnDefinition = "CHAR(13)")
	private String scheduleTsid;

	@Column(name = "created_at", nullable = false, updatable = false)
	@CreatedDate
	private Instant createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "gathering_tsid",
		insertable = false,
		updatable = false,
		foreignKey = @ForeignKey(name = "fk_chat_room_gathering")
	)
	private GatheringEntity gathering;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "schedule_tsid",
		insertable = false,
		updatable = false,
		foreignKey = @ForeignKey(name = "fk_chat_room_schedule")
	)
	private ScheduleEntity schedule;

	/**
	 * 모임 채팅방 생성
	 *
	 * @param gatheringTsid 모임 TSID
	 */
	public static ChatRoomEntity forGathering(String gatheringTsid) {
		return ChatRoomEntity.builder()
			.roomType(ChatRoomType.GATHERING)
			.gatheringTsid(gatheringTsid)
			.build();
	}

	/**
	 * 일정 채팅방 생성
	 *
	 * @param scheduleTsid 일정 TSID
	 */
	public static ChatRoomEntity forSchedule(String scheduleTsid) {
		return ChatRoomEntity.builder()
			.roomType(ChatRoomType.SCHEDULE)
			.scheduleTsid(scheduleTsid)
			.build();
	}

	/**
	 * 이 채팅방이 붙어 있는 주체(모임 또는 일정)의 TSID
	 * 멤버십 판정 시 roomType 과 함께 사용한다
	 */
	public String getOwnerTsid() {
		return switch (roomType) {
			case GATHERING -> gatheringTsid;
			case SCHEDULE -> scheduleTsid;
		};
	}
}
