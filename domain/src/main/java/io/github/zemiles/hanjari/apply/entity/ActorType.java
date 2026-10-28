package io.github.zemiles.hanjari.apply.entity;

/**
 * 신청 상태를 바꾼 주체. apply_history에 기록한다.
 */
public enum ActorType {

	/** 신청한 본인 */
	SELF,

	/** 신청 관리 권한이 있는 사람 (클럽장, 부클럽장, 권한을 받은 운영진) */
	STAFF,

	/** 사람이 아닌 시스템 처리 (자동 승격, 만료 등). 바꾼 사람 id는 비어 있다 */
	SYSTEM
}
