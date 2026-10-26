# 한자리

정기 모임 운영자를 위한 선착순 신청, 출석, 노쇼 관리 서비스입니다.

## 기술 스택

Java 21, Spring Boot 4.1, PostgreSQL 18, Redis 8.2, Kafka 4.3, Gradle (멀티 모듈)

## 모듈 구조

| 모듈     | 역할                  |
|--------|---------------------|
| domain | 도메인, 영속성, 공통 설정     |
| api    | 신청·취소·승인·출석 API     |
| worker | 아웃박스 릴레이, Kafka 컨슈머 |
| batch  | 정합성 대사, 만료 처리       |

## 로컬 실행

필요한 것: JDK 21, Docker

```bash
cp .env.example .env        # POSTGRES_PASSWORD 채우기
docker compose up -d
./gradlew build
./gradlew :api:bootRun
```

테스트는 Testcontainers로 PostgreSQL을 직접 띄우기 때문에 Docker만 켜져 있으면 compose 없이도 실행됩니다.
