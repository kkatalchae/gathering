package com.gathering.chat.domain.policy;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gathering.chat.domain.model.ChatRoomEntity;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.gathering.domain.repository.GatheringParticipantRepository;
import com.gathering.schedule.domain.repository.ScheduleParticipantRepository;

/**
 * ChatRoomPolicy 테스트 — 멤버십이 방 타입에 맞는 참여자 테이블에서 파생되는지 검증한다
 */
@ExtendWith(MockitoExtension.class)
class ChatRoomPolicyTest {

	private static final String USER_TSID = "01HQUSER000001";

	@Mock
	private GatheringParticipantRepository gatheringParticipantRepository;

	@Mock
	private ScheduleParticipantRepository scheduleParticipantRepository;

	@InjectMocks
	private ChatRoomPolicy policy;

	@Test
	@DisplayName("모임 채팅방은 모임 참여자만 멤버다")
	void gatheringRoomMembership() {
		// given
		ChatRoomEntity room = ChatRoomEntity.forGathering("01HQGATHERING1");
		given(gatheringParticipantRepository.existsByGatheringTsidAndUserTsid("01HQGATHERING1", USER_TSID))
			.willReturn(true);

		// when & then
		assertThatCode(() -> policy.validateMember(room, USER_TSID)).doesNotThrowAnyException();
		then(scheduleParticipantRepository).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("일정 채팅방은 일정 참여자만 멤버다")
	void scheduleRoomMembership() {
		// given
		ChatRoomEntity room = ChatRoomEntity.forSchedule("01HQSCHEDULE01");
		given(scheduleParticipantRepository.existsByScheduleTsidAndUserTsid("01HQSCHEDULE01", USER_TSID))
			.willReturn(true);

		// when & then
		assertThatCode(() -> policy.validateMember(room, USER_TSID)).doesNotThrowAnyException();
		then(gatheringParticipantRepository).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("참여자가 아니면 채팅방 접근이 거부된다")
	void nonMemberIsDenied() {
		// given
		ChatRoomEntity room = ChatRoomEntity.forGathering("01HQGATHERING1");
		given(gatheringParticipantRepository.existsByGatheringTsidAndUserTsid("01HQGATHERING1", USER_TSID))
			.willReturn(false);

		// when & then
		assertThatThrownBy(() -> policy.validateMember(room, USER_TSID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_ACCESS_DENIED);
	}
}
