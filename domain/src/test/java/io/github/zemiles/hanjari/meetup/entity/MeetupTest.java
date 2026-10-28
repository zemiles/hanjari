package io.github.zemiles.hanjari.meetup.entity;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Meetup 관련 테스트")
public class MeetupTest {

	private static final Instant APPLY_OPENS_AT = Instant.parse("2026-10-28T01:00:00Z");

	private static final Instant APPLY_CLOSES_AT = Instant.parse("2026-10-29T01:00:00Z");

	private static final Instant STARTS_AT = Instant.parse("2026-10-30T10:00:00Z");

	@Test
	@DisplayName("선택값을 넘기지 않으면 기본값으로 만든다")
	void createWithDefaults() {
		Meetup meetup = requiredOnly().build();

		assertThat(meetup.isWaitlistEnabled()).isTrue();
		assertThat(meetup.getAttendanceWindowMinutes()).isEqualTo(60);
		assertThat(meetup.getQrRadiusMeters()).isEqualTo(100);
		assertThat(meetup.getWaitlistCapacity()).isNull(); // 비어 있으면 대기 정원 제한 없음
		assertThat(meetup.getManualFallbackMinutes()).isNull();
	}

	@Test
	@DisplayName("선택값을 넘기면 넘긴 값으로 만든다")
	void createWithValues() {
		Meetup meetup = requiredOnly()
			.waitlistEnabled(false)
			.attendanceWindowMinutes(30)
			.qrRadiusMeters(200)
			.build();

		assertThat(meetup.isWaitlistEnabled()).isFalse();
		assertThat(meetup.getAttendanceWindowMinutes()).isEqualTo(30);
		assertThat(meetup.getQrRadiusMeters()).isEqualTo(200);
	}

	@Test
	@DisplayName("새 회차는 예정 상태이고 카운터는 0, 0, 1로 시작한다")
	void createScheduled() {
		Meetup meetup = requiredOnly().build();

		assertThat(meetup.getStatus()).isEqualTo(MeetupStatus.SCHEDULED);
		assertThat(meetup.getCancelledAt()).isNull();
		assertThat(meetup.getAttendanceOpenedAt()).isNull();
		assertThat(meetup.getConfirmedCount()).isZero();
		assertThat(meetup.getWaitingCount()).isZero();
		assertThat(meetup.getNextWaitingSeq()).isEqualTo(1);
	}

	// 필수값만 채운 builder
	private Meetup.MeetupBuilder requiredOnly() {
		return Meetup.builder()
			.clubId(1L)
			.title("10월 정기전")
			.startsAt(STARTS_AT)
			.capacity(10)
			.applyOpensAt(APPLY_OPENS_AT)
			.applyClosesAt(APPLY_CLOSES_AT)
			.promotionMode(PromotionMode.AUTO);
	}

}
