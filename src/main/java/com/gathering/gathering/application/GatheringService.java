package com.gathering.gathering.application;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.gathering.domain.model.GatheringDescription;
import com.gathering.gathering.domain.model.GatheringEntity;
import com.gathering.gathering.domain.model.GatheringName;
import com.gathering.gathering.domain.model.GatheringParticipantEntity;
import com.gathering.gathering.domain.model.ParticipantRole;
import com.gathering.gathering.domain.policy.GatheringPolicy;
import com.gathering.gathering.domain.repository.GatheringParticipantRepository;
import com.gathering.gathering.domain.repository.GatheringRepository;
import com.gathering.gathering.presentation.dto.ChangeParticipantRoleRequest;
import com.gathering.gathering.presentation.dto.ChangeParticipantRoleResponse;
import com.gathering.gathering.presentation.dto.CreateGatheringRequest;
import com.gathering.gathering.presentation.dto.GatheringDetailResponse;
import com.gathering.gathering.presentation.dto.GatheringListItemResponse;
import com.gathering.gathering.presentation.dto.GatheringListRequest;
import com.gathering.gathering.presentation.dto.GatheringListResponse;
import com.gathering.gathering.presentation.dto.GatheringResponse;
import com.gathering.gathering.presentation.dto.UpdateGatheringRequest;

import lombok.RequiredArgsConstructor;

/**
 * 모임 Application Service
 * DDD 원칙에 따라 유스케이스 조립만 담당 (검증은 VO와 Policy가 처리)
 */
@Service
@RequiredArgsConstructor
public class GatheringService {

	private final GatheringRepository gatheringRepository;
	private final GatheringParticipantRepository participantRepository;
	private final GatheringPolicy gatheringPolicy;
	private final GatheringParticipantService gatheringParticipantService;

	/**
	 * 모임 생성
	 * 모임을 생성하고 생성자를 자동으로 OWNER로 등록
	 *
	 * @param ownerTsid 모임을 생성하는 사용자 TSID
	 * @param request 모임 생성 요청 DTO
	 * @return 생성된 모임 정보
	 */
	@Transactional
	public GatheringResponse createGathering(String ownerTsid, CreateGatheringRequest request) {

		// 도메인 규칙 검증
		String name = request.getName();
		String description = request.getDescription();
		String regionTsid = request.getRegionTsid();
		validateGatheringPolicy(name, description, regionTsid);

		// Gathering 엔티티 생성 및 저장
		GatheringEntity gathering = GatheringEntity.builder()
			.name(name)
			.description(description)
			.regionTsid(regionTsid)
			.category(request.getCategory())
			.mainImageUrl(request.getMainImageUrl())
			.maxParticipants(100)
			.build();
		GatheringEntity savedGathering = gatheringRepository.save(gathering);

		// Owner로 Participant 등록
		GatheringParticipantEntity owner = GatheringParticipantEntity.builder()
			.gatheringTsid(savedGathering.getTsid())
			.userTsid(ownerTsid)
			.role(ParticipantRole.OWNER)
			.build();
		participantRepository.save(owner);

		return GatheringResponse.from(savedGathering);
	}

	/**
	 * 모임 목록 조회
	 * 카테고리와 지역으로 필터링하며 커서 기반 페이지네이션 지원
	 * 지역 필터링은 계층적으로 동작 (부모 지역 선택 시 하위 지역 모임도 포함)
	 *
	 * @param request 필터링 및 페이지네이션 요청
	 * @return 모임 목록 및 다음 커서 정보
	 */
	@Transactional(readOnly = true)
	public GatheringListResponse getGatherings(GatheringListRequest request) {
		// size+1로 조회하여 hasNext 판단
		Pageable pageable = PageRequest.of(0, request.getSize() + 1);
		List<GatheringEntity> gatherings = gatheringRepository.findGatheringsWithFilters(
			request.getCategories(),
			request.getRegionTsids(),
			request.getCursor(),
			pageable
		);

		// hasNext 계산
		boolean hasNext = gatherings.size() > request.getSize();
		if (hasNext) {
			gatherings = gatherings.subList(0, request.getSize());
		}

		// DTO 변환
		List<GatheringListItemResponse> items = gatherings.stream()
			.map(GatheringListItemResponse::from)
			.toList();

		// 다음 커서 생성
		String nextCursor = hasNext ? gatherings.getLast().getTsid() : null;

		return GatheringListResponse.of(items, nextCursor, hasNext);
	}

	/**
	 * 모임 상세 조회
	 * 모임 정보와 전체 참여자 리스트 포함
	 *
	 * @param tsid 모임 TSID
	 * @return 모임 상세 정보
	 */
	@Transactional(readOnly = true)
	public GatheringDetailResponse getGatheringDetail(String tsid) {
		GatheringEntity gathering = gatheringRepository.findById(tsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_NOT_FOUND));

		List<GatheringParticipantEntity> participants = participantRepository.findAllByGatheringTsidWithUser(tsid);

		return GatheringDetailResponse.from(gathering, participants);
	}

	/**
	 * 모임 정보 수정
	 * OWNER 또는 ADMIN만 수정 가능
	 *
	 * @param gatheringTsid 수정할 모임 TSID
	 * @param userTsid 요청 사용자 TSID
	 * @param request 수정 요청 DTO
	 * @return 수정된 모임 정보
	 */
	@Transactional
	public GatheringResponse updateGathering(String gatheringTsid, String userTsid,
		UpdateGatheringRequest request) {
		// 모임 존재 여부 확인
		GatheringEntity gathering = gatheringRepository.findById(gatheringTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_NOT_FOUND));

		// 권한 검증 (OWNER/ADMIN만 가능)
		gatheringPolicy.validateUpdatePermission(gatheringTsid, userTsid);

		// 도메인 규칙 검증
		String name = request.getName();
		String description = request.getDescription();
		String regionTsid = request.getRegionTsid();
		validateGatheringPolicy(name, description, regionTsid);

		// 엔티티 수정
		gathering.update(
			name,
			description,
			regionTsid,
			request.getCategory(),
			request.getMainImageUrl()
		);

		// 응답 반환 (Dirty Checking으로 자동 저장)
		return GatheringResponse.from(gathering);
	}

	/**
	 * 모임 삭제
	 * OWNER만 삭제 가능하며 모임과 관련 참여자 데이터가 함께 삭제됨
	 *
	 * @param gatheringTsid 삭제할 모임 TSID
	 * @param userTsid 요청 사용자 TSID
	 */
	@Transactional
	public void deleteGathering(String gatheringTsid, String userTsid) {
		// 모임 존재 여부 확인
		if (!gatheringRepository.existsById(gatheringTsid)) {
			throw new BusinessException(ErrorCode.GATHERING_NOT_FOUND);
		}

		// 권한 검증 (OWNER만 가능)
		gatheringPolicy.validateOwnerPermission(gatheringTsid, userTsid);

		// 참여자 데이터 삭제 (FK 제약 조건으로 인해 먼저 삭제)
		participantRepository.deleteAllByGatheringTsid(gatheringTsid);

		// 모임 삭제
		gatheringRepository.deleteById(gatheringTsid);
	}

	/**
	 * 참여자 역할 변경
	 * OWNER만 역할 변경 가능하며 OWNER 양도 시 자신은 ADMIN으로 자동 변경
	 *
	 * @param gatheringTsid 모임 TSID
	 * @param requesterTsid 요청자 TSID (현재 로그인 사용자)
	 * @param targetUserTsid 대상 사용자 TSID
	 * @param request 역할 변경 요청
	 * @return 역할 변경 결과
	 */
	@Transactional
	public ChangeParticipantRoleResponse changeParticipantRole(
		String gatheringTsid,
		String requesterTsid,
		String targetUserTsid,
		ChangeParticipantRoleRequest request) {

		// 자기 자신의 역할 변경 방지
		if (requesterTsid.equals(targetUserTsid)) {
			throw new BusinessException(ErrorCode.CANNOT_CHANGE_OWN_ROLE);
		}

		// 모임 존재 여부 확인
		if (!gatheringRepository.existsById(gatheringTsid)) {
			throw new BusinessException(ErrorCode.GATHERING_NOT_FOUND);
		}

		// 요청자 권한 검증 (OWNER만 가능)
		GatheringParticipantEntity requester = gatheringPolicy.validateOwnerPermission(gatheringTsid, requesterTsid);

		// 대상 참여자 검증
		GatheringParticipantEntity target = gatheringParticipantService.findParticipants(gatheringTsid, targetUserTsid);

		ParticipantRole previousRole = target.getRole();
		ParticipantRole newRole = request.getNewRole();

		// OWNER 양도 처리
		if (newRole == ParticipantRole.OWNER) {
			// 현재 OWNER(요청자)를 ADMIN으로 변경
			requester.changeRole(ParticipantRole.ADMIN);
		}

		target.changeRole(newRole);

		return ChangeParticipantRoleResponse.of(
			target.getTsid(),
			target.getUserTsid(),
			previousRole,
			newRole
		);
	}

	private void validateGatheringPolicy(String name, String description, String regionTsid) {

		// Value Object 생성 (값 유효성 자동 검증)
		new GatheringName(name);
		new GatheringDescription(description);

		// 지역 존재 여부 검증
		gatheringPolicy.validateRegionExists(regionTsid);

	}
}
