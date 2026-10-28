package io.github.zemiles.hanjari.attendance.entity;

/**
 * 확정된 신청 한 건의 당일 출석 결과.
 */
public enum AttendanceStatus {

	/** 출석: 회차 시작 전에 체크했다 */
	ATTENDED,

	/** 지각: 시작 후 출석 인정 시간 안에 체크했다. 기록만 남고 패널티는 없다 */
	LATE,

	/** 노쇼: 권한 있는 사람이 확정한다. 확정 뒤에는 출석으로 바꾸지 않는다 */
	NO_SHOW
}
