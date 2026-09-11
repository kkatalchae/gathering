package com.gathering.chat.domain.model;

import java.io.Serializable;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 읽음 위치 복합 키 — 사용자로 파티션을 고르고 방으로 클러스터링해 "내 방들의 읽음 위치" 를 한 파티션에서 읽는다
 */
@PrimaryKeyClass
@Getter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomReadPositionKey implements Serializable {

	@PrimaryKeyColumn(name = "user_tsid", type = PrimaryKeyType.PARTITIONED, ordinal = 0)
	private String userTsid;

	@PrimaryKeyColumn(name = "room_tsid", type = PrimaryKeyType.CLUSTERED, ordinal = 1)
	private String roomTsid;
}
