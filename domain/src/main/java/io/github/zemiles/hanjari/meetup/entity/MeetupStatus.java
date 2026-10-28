package io.github.zemiles.hanjari.meetup.entity;

/**
 * 회차의 상태.
 */
public enum MeetupStatus {

	/** 예정 */
	SCHEDULED,

	/** 취소: 진행 중이던 신청은 모두 회차 취소(MEETUP_CANCELLED)로 바뀐다 */
	CANCELLED
}
