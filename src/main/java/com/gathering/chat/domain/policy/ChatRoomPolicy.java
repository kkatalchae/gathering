package com.gathering.chat.domain.policy;

import org.springframework.stereotype.Component;

import com.gathering.chat.domain.model.ChatRoomEntity;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.gathering.domain.repository.GatheringParticipantRepository;
import com.gathering.schedule.domain.repository.ScheduleParticipantRepository;

import lombok.RequiredArgsConstructor;

/**
 * 채팅방 도메인 규칙 — 멤버십은 저장하지 않고 주체(모임/일정)의 참여자 테이블에서 판정한다 (docs/adr/0004)
 */
@Component
@RequiredArgsConstructor
public class ChatRoomPolicy {

	private final GatheringParticipantRepository gatheringParticipantRepository;
	private final ScheduleParticipantRepository scheduleParticipantRepository;

	/**
	 * 채팅방 멤버인지 검증 — 메시지 조회·전송·읽음 처리 모두 이 검증을 거친다
	 *
	 * @throws BusinessException 주체에 참여하고 있지 않은 사용자인 경우
	 */
	public void validateMember(ChatRoomEntity room, String userTsid) {
		if (!isMember(room, userTsid)) {
			throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
		}
	}

	public boolean isMember(ChatRoomEntity room, String userTsid) {
		// 타입별로 명시 분기한다 — 값이 늘면 컴파일 에러로 드러나야 한다
		return switch (room.getRoomType()) {
			case GATHERING -> gatheringParticipantRepository
				.existsByGatheringTsidAndUserTsid(room.getGatheringTsid(), userTsid);
			case SCHEDULE -> scheduleParticipantRepository
				.existsByScheduleTsidAndUserTsid(room.getScheduleTsid(), userTsid);
		};
	}
}
