package com.gathering.chat.domain.model;

import java.io.Serializable;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅 메시지 복합 키
 * 파티션 키 (room_tsid, bucket) 으로 방·월 단위 파티션을 고르고, 클러스터링 컬럼 message_tsid 로 최신순 정렬한다
 */
@PrimaryKeyClass
@Getter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageKey implements Serializable {

	@PrimaryKeyColumn(name = "room_tsid", type = PrimaryKeyType.PARTITIONED, ordinal = 0)
	private String roomTsid;

	@PrimaryKeyColumn(name = "bucket", type = PrimaryKeyType.PARTITIONED, ordinal = 1)
	private String bucket;

	@PrimaryKeyColumn(name = "message_tsid", type = PrimaryKeyType.CLUSTERED, ordinal = 2, ordering = Ordering.DESCENDING)
	private String messageTsid;
}
