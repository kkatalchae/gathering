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
import com.gathering.gathering.presentation.dto.CreateGatheringRequest;
import com.gathering.gathering.presentation.dto.CreateGatheringResponse;
import com.gathering.gathering.presentation.dto.GatheringDetailResponse;
import com.gathering.gathering.presentation.dto.GatheringListItemResponse;
import com.gathering.gathering.presentation.dto.GatheringListRequest;
import com.gathering.gathering.presentation.dto.GatheringListResponse;

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

	/**
	 * 모임 생성
	 * 모임을 생성하고 생성자를 자동으로 OWNER로 등록
	 *
	 * @param ownerTsid 모임을 생성하는 사용자 TSID
	 * @param request 모임 생성 요청 DTO
	 * @return 생성된 모임 정보
	 */
	@Transactional(readOnly = false)
	public CreateGatheringResponse createGathering(String ownerTsid, CreateGatheringRequest request) {
		// Value Object 생성 (값 유효성 자동 검증)
		GatheringName name = new GatheringName(request.getName());
		GatheringDescription description = new GatheringDescription(request.getDescription());

		// Domain Policy 검증
		gatheringPolicy.validateRegionExists(request.getRegionTsid());

		// Gathering 엔티티 생성 및 저장
		GatheringEntity gathering = GatheringEntity.builder()
			.name(name.getValue())
			.description(description.getValue())
			.regionTsid(request.getRegionTsid())
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

		// 응답 반환
		return CreateGatheringResponse.from(savedGathering);
	}

	/**
	 * 모임 목록 조회
	 * 카테고리와 지역으로 필터링하며 커서 기반 페이지네이션 지원
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
		String nextCursor = hasNext ? gatherings.get(gatherings.size() - 1).getTsid() : null;

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
}