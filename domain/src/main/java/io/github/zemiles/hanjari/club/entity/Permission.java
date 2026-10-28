package io.github.zemiles.hanjari.club.entity;

/**
 * 운영진에게 켜고 끌 수 있는 권한. 클럽장과 부클럽장은 역할로 모두 가진다.
 */
public enum Permission {

	/** 회차 관리: 회차 생성, 수정, 취소 */
	MEETUP_MANAGE,

	/** 신청 관리: 신청 거절, 수동 승격 */
	APPLY_MANAGE,

	/** 출석 관리: 출석 대신 처리, 노쇼 확정 */
	ATTENDANCE_MANAGE,

	/** 멤버 관리: 가입 승인, 내보내기 */
	MEMBER_MANAGE
}
