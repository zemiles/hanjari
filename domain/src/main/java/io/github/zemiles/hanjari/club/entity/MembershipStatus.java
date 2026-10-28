package io.github.zemiles.hanjari.club.entity;

/**
 * 클럽 소속의 상태.
 */
public enum MembershipStatus {

	/** 가입 대기: 가입을 신청하고 승인을 기다리는 상태 */
	PENDING,

	/** 활동 */
	ACTIVE,

	/** 클럽 탈퇴: 본인이 나갔다. 다시 가입하면 이 줄을 다시 쓴다 */
	LEFT,

	/** 내보냄: 멤버 관리 권한이 있는 사람이 내보냈다 */
	KICKED
}
