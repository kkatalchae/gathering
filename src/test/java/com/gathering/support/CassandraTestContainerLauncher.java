package com.gathering.support;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.cassandra.CassandraContainer;

/**
 * 테스트 JVM 당 Cassandra 컨테이너를 한 번만 띄우는 JUnit Platform 리스너
 *
 * Spring Boot 는 시작 시 Cassandra 에 연결하므로 모든 @SpringBootTest 가 살아 있는 Cassandra 를 필요로 한다.
 * 테스트 클래스마다 컨테이너를 띄우면 기동 비용(30초↑)이 반복되므로, 런처 세션이 열릴 때 하나를 띄우고
 * 접속 정보를 시스템 프로퍼티로 넣어 모든 컨텍스트가 공유하게 한다.
 * META-INF/services/org.junit.platform.launcher.LauncherSessionListener 로 등록된다.
 *
 * Docker 가 없거나 기동에 실패하면 예외를 삼키고 경고만 남긴다 — 그래야 Spring 컨텍스트가 필요 없는
 * VO/정책 단위 테스트는 그대로 돌고, Spring 테스트만 "연결 실패" 로 명확히 실패한다.
 * 예외를 던지면 Gradle 테스트 워커 자체가 죽어 모든 테스트가 원인 없는 실패로 보인다. docs/adr/0002 참고.
 */
public class CassandraTestContainerLauncher implements LauncherSessionListener {

	private static final Logger log = LoggerFactory.getLogger(CassandraTestContainerLauncher.class);

	private static final String KEYSPACE_INIT_SCRIPT = "cassandra-init.cql";

	// 환경변수로 덮어쓸 수 있다 — docker-compose 와 같은 키에 TEST_ 접두어
	private static final String IMAGE = env("CASSANDRA_TEST_IMAGE", "cassandra:5.0");
	private static final String MAX_HEAP_SIZE = env("CASSANDRA_TEST_MAX_HEAP_SIZE", "512M");
	private static final String HEAP_NEWSIZE = env("CASSANDRA_TEST_HEAP_NEWSIZE", "128M");

	private CassandraContainer container;

	@Override
	public void launcherSessionOpened(LauncherSession session) {
		CassandraContainer candidate = new CassandraContainer(IMAGE)
			.withInitScript(KEYSPACE_INIT_SCRIPT)
			.withEnv("MAX_HEAP_SIZE", MAX_HEAP_SIZE)
			.withEnv("HEAP_NEWSIZE", HEAP_NEWSIZE);

		try {
			candidate.start();
		} catch (RuntimeException e) {
			log.warn("""

				==========================================================================
				 Cassandra 테스트 컨테이너를 띄우지 못했습니다. Docker 가 실행 중인지 확인하세요.
				 Spring 컨텍스트를 쓰는 테스트(@SpringBootTest, @DataCassandraTest)는 연결 실패로 실패하고,
				 순수 단위 테스트만 실행됩니다. 원인: {}
				==========================================================================
				""", e.getMessage());
			return;
		}

		container = candidate;
		System.setProperty("spring.cassandra.contact-points",
			container.getHost() + ":" + container.getMappedPort(9042));
		System.setProperty("spring.cassandra.local-datacenter", container.getLocalDatacenter());
	}

	private static String env(String name, String defaultValue) {
		String value = System.getenv(name);
		return value == null || value.isBlank() ? defaultValue : value;
	}

	@Override
	public void launcherSessionClosed(LauncherSession session) {
		if (container != null) {
			container.stop();
		}
	}
}
