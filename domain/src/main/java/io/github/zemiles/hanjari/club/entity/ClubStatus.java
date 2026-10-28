package io.github.zemiles.hanjari.club.entity;

/**
 * 클럽의 운영 상태.
 */
public enum ClubStatus {

	/** 활동 */
	ACTIVE,

	/** 폐쇄 예고: 폐쇄 예정일(closes_at)이 공지된 상태 */
	CLOSING,

	/** 폐쇄 */
	CLOSED
}
