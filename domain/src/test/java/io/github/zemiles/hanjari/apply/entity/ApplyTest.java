package io.github.zemiles.hanjari.apply.entity;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Apply 관련 테스트")
public class ApplyTest {

	private static final Instant NOW = Instant.parse("2026-10-28T01:00:00Z");

	private static final Long MEETUP_ID = 1L;

	private static final Long MEMBER_ID = 10L;

	private static final String IDEMPOTENCY_KEY = "apply-key-1";

	@Test
	@DisplayName("확정 신청은 받은 시각을 신청 시각과 확정 시각에 넣는다")
	void createConfirmed() {
		Apply apply = Apply.createConfirmed(MEETUP_ID, MEMBER_ID, IDEMPOTENCY_KEY, NOW);

		assertThat(apply.getMeetupId()).isEqualTo(MEETUP_ID);
		assertThat(apply.getMemberId()).isEqualTo(MEMBER_ID);
		assertThat(apply.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
		assertThat(apply.getStatus()).isEqualTo(ApplyStatus.CONFIRMED);
		assertThat(apply.getAppliedAt()).isEqualTo(NOW);
		assertThat(apply.getConfirmedAt()).isEqualTo(NOW);
		assertThat(apply.getWaitingSeq()).isNull();
		assertThat(apply.getEndedAt()).isNull();
	}

	@Test
	@DisplayName("대기 신청은 대기 순번을 넣고 확정 시각은 비워 둔다")
	void createWaiting() {
		Apply apply = Apply.createWaiting(MEETUP_ID, MEMBER_ID, IDEMPOTENCY_KEY, 3, NOW);

		assertThat(apply.getStatus()).isEqualTo(ApplyStatus.WAITING);
		assertThat(apply.getWaitingSeq()).isEqualTo(3);
		assertThat(apply.getAppliedAt()).isEqualTo(NOW);
		assertThat(apply.getConfirmedAt()).isNull();
		assertThat(apply.getEndedAt()).isNull();
	}

}
