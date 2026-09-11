package com.gathering.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing (@CreatedDate, @LastModifiedDate) 활성화
 * 애플리케이션 클래스가 아니라 별도 설정으로 둔 이유: @DataCassandraTest 처럼 JPA 가 없는 슬라이스 테스트가
 * 애플리케이션 클래스의 어노테이션을 그대로 물려받아 "JPA metamodel must not be empty" 로 실패하기 때문이다.
 * @DataJpaTest 는 이 설정을 @Import 해서 쓴다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
