package com.gathering.gathering.application;

import org.springframework.stereotype.Service;

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

import jakarta.transaction.Transactional;
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
	@Transactional
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
}