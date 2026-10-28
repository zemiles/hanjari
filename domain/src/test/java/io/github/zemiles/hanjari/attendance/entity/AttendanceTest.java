package io.github.zemiles.hanjari.attendance.entity;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Attendance 관련 테스트")
public class AttendanceTest {

	private static final Long APPLY_ID = 1L;

	private static final Long STAFF_ID = 20L;

	private static final Instant STARTS_AT = Instant.parse("2026-10-30T10:00:00Z");

	private static final Instant BEFORE_START = Instant.parse("2026-10-30T09:50:00Z");

	private static final Instant AFTER_START = Instant.parse("2026-10-30T10:10:00Z");

	@Test
	@DisplayName("회차 시작 전에 셀프 체크하면 출석이다")
	void selfCheckBeforeStart() {
		Attendance attendance = Attendance.selfCheck(APPLY_ID, STARTS_AT, BEFORE_START, 30);

		assertThat(attendance.getApplyId()).isEqualTo(APPLY_ID);
		assertThat(attendance.getStatus()).isEqualTo(AttendanceStatus.ATTENDED);
		assertThat(attendance.getCheckedAt()).isEqualTo(BEFORE_START);
		assertThat(attendance.getCheckMethod()).isEqualTo(CheckMethod.SELF);
		assertThat(attendance.getDistanceMeters()).isEqualTo(30);
		assertThat(attendance.getProcessedBy()).isNull();
	}

	@Test
	@DisplayName("회차 시작 시각 정각에 셀프 체크하면 출석이다")
	void selfCheckAtStart() {
		Attendance attendance = Attendance.selfCheck(APPLY_ID, STARTS_AT, STARTS_AT, 30);

		assertThat(attendance.getStatus()).isEqualTo(AttendanceStatus.ATTENDED);
	}

	@Test
	@DisplayName("회차 시작 후에 셀프 체크하면 지각이다")
	void selfCheckAfterStart() {
		Attendance attendance = Attendance.selfCheck(APPLY_ID, STARTS_AT, AFTER_START, 30);

		assertThat(attendance.getStatus()).isEqualTo(AttendanceStatus.LATE);
	}

	@Test
	@DisplayName("회차 시작 전에 대신 처리하면 출석이고 처리한 사람이 남는다")
	void staffCheckBeforeStart() {
		Attendance attendance = Attendance.staffCheck(APPLY_ID, STAFF_ID, STARTS_AT, BEFORE_START);

		assertThat(attendance.getStatus()).isEqualTo(AttendanceStatus.ATTENDED);
		assertThat(attendance.getCheckedAt()).isEqualTo(BEFORE_START);
		assertThat(attendance.getCheckMethod()).isEqualTo(CheckMethod.STAFF);
		assertThat(attendance.getProcessedBy()).isEqualTo(STAFF_ID);
		assertThat(attendance.getDistanceMeters()).isNull();
	}

	@Test
	@DisplayName("회차 시작 후에 대신 처리하면 지각이다")
	void staffCheckAfterStart() {
		Attendance attendance = Attendance.staffCheck(APPLY_ID, STAFF_ID, STARTS_AT, AFTER_START);

		assertThat(attendance.getStatus()).isEqualTo(AttendanceStatus.LATE);
	}

	@Test
	@DisplayName("노쇼는 체크 시각과 체크 방식을 비워 두고 확정한 사람을 남긴다")
	void noShow() {
		Attendance attendance = Attendance.noShow(APPLY_ID, STAFF_ID);

		assertThat(attendance.getStatus()).isEqualTo(AttendanceStatus.NO_SHOW);
		assertThat(attendance.getCheckedAt()).isNull();
		assertThat(attendance.getCheckMethod()).isNull();
		assertThat(attendance.getProcessedBy()).isEqualTo(STAFF_ID);
		assertThat(attendance.getDistanceMeters()).isNull();
	}

}
