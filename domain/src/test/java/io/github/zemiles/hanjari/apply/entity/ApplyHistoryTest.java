package io.github.zemiles.hanjari.apply.entity;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ApplyHistory 관련 테스트")
public class ApplyHistoryTest {

	private static final Long APPLICANT_ID = 10L;

	private static final Long STAFF_ID = 20L;

	private final Apply apply = Apply.createConfirmed(1L, APPLICANT_ID, "apply-key-1",
		Instant.parse("2026-10-28T01:00:00Z"));

	@Test
	@DisplayName("본인이 바꾼 이력은 신청한 사람을 바꾼 사람으로 남긴다")
	void createBySelf() {
		ApplyHistory history = ApplyHistory.bySelf(apply, null, ApplyStatus.CONFIRMED);

		assertThat(history.getApply()).isSameAs(apply);
		assertThat(history.getFromStatus()).isNull();
		assertThat(history.getToStatus()).isEqualTo(ApplyStatus.CONFIRMED);
		assertThat(history.getActorType()).isEqualTo(ActorType.SELF);
		assertThat(history.getActorMemberId()).isEqualTo(APPLICANT_ID);
	}

	@Test
	@DisplayName("권한 있는 사람이 바꾼 이력은 넘긴 사람을 바꾼 사람으로 남긴다")
	void createByStaff() {
		ApplyHistory history = ApplyHistory.byStaff(apply, ApplyStatus.WAITING, ApplyStatus.CONFIRMED, STAFF_ID);

		assertThat(history.getFromStatus()).isEqualTo(ApplyStatus.WAITING);
		assertThat(history.getToStatus()).isEqualTo(ApplyStatus.CONFIRMED);
		assertThat(history.getActorType()).isEqualTo(ActorType.STAFF);
		assertThat(history.getActorMemberId()).isEqualTo(STAFF_ID);
	}

	@Test
	@DisplayName("시스템이 바꾼 이력은 바꾼 사람을 비워 둔다")
	void createBySystem() {
		ApplyHistory history = ApplyHistory.bySystem(apply, ApplyStatus.WAITING, ApplyStatus.CONFIRMED);

		assertThat(history.getActorType()).isEqualTo(ActorType.SYSTEM);
		assertThat(history.getActorMemberId()).isNull();
	}

}
