package com.gathering.chat.application;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.gathering.chat.presentation.dto.ChatSenderSummary;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

/**
 * 메시지 발신자 정보 조립 — 발신자 TSID 를 모아 users 를 IN 한 번으로 조회한다 (조인이 없는 Cassandra 의 대안, 메시지별 반복 조회 금지)
 * 조회 시점의 사용자 정보를 쓰므로 탈퇴한 사용자는 익명화된 이름으로 보인다 (docs/adr/0002)
 */
@Component
@RequiredArgsConstructor
public class ChatSenderResolver {

	private final UsersRepository usersRepository;

	/**
	 * @param senderTsids 발신자 TSID 들 (null 은 SYSTEM 메시지이므로 무시, 중복 허용)
	 * @return 발신자 TSID → 요약. 존재하지 않는 사용자는 빠진다
	 */
	public Map<String, ChatSenderSummary> resolve(Collection<String> senderTsids) {
		List<String> distinct = senderTsids.stream()
			.filter(Objects::nonNull)
			.distinct()
			.toList();
		if (distinct.isEmpty()) {
			return Map.of();
		}
		return usersRepository.findAllById(distinct).stream()
			.collect(Collectors.toMap(UsersEntity::getTsid, ChatSenderSummary::from, (a, b) -> a));
	}
}
