-- 로컬 개발용 키스페이스. 테이블은 애플리케이션이 엔티티에서 생성한다 (spring.cassandra.schema-action)
CREATE KEYSPACE IF NOT EXISTS gathering
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
