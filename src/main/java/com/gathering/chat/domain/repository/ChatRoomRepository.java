package com.gathering.chat.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gathering.chat.domain.model.ChatRoomEntity;

/**
 * 채팅방 메타데이터 (MySQL) — 모임/일정과의 연결과 멤버십 파생의 출발점
 */
public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, String> {

	Optional<ChatRoomEntity> findByGatheringTsid(String gatheringTsid);

	Optional<ChatRoomEntity> findByScheduleTsid(String scheduleTsid);

	List<ChatRoomEntity> findAllByScheduleTsidIn(List<String> scheduleTsids);

	/**
	 * 여러 모임의 채팅방을 모임과 함께 조회 (내 채팅방 목록 — 방 이름을 모임에서 가져오므로 fetch join)
	 */
	@Query("SELECT r FROM ChatRoomEntity r JOIN FETCH r.gathering WHERE r.gatheringTsid IN :gatheringTsids")
	List<ChatRoomEntity> findAllByGatheringTsidInWithGathering(@Param("gatheringTsids") List<String> gatheringTsids);

	/**
	 * 여러 일정의 채팅방을 일정과 함께 조회 (내 채팅방 목록)
	 */
	@Query("SELECT r FROM ChatRoomEntity r JOIN FETCH r.schedule WHERE r.scheduleTsid IN :scheduleTsids")
	List<ChatRoomEntity> findAllByScheduleTsidInWithSchedule(@Param("scheduleTsids") List<String> scheduleTsids);

	/**
	 * 채팅방 row 일괄 삭제 (모임/일정 삭제 시). Cassandra 메시지 파티션은 커밋 후 별도로 지운다
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("DELETE FROM ChatRoomEntity r WHERE r.tsid IN :roomTsids")
	void deleteAllByTsidIn(@Param("roomTsids") List<String> roomTsids);
}
