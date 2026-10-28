package io.github.zemiles.hanjari.apply.entity;

/**
 * 회차 신청의 상태. WAITING과 CONFIRMED는 진행 중, 나머지는 끝난 상태다.
 */
public enum ApplyStatus {

	/** 대기: 정원이 차서 대기 순번을 받은 상태 */
	WAITING,

	/** 확정: 자리를 받은 상태. 출석한 뒤에도 확정으로 남는다 */
	CONFIRMED,

	/** 본인 취소 */
	CANCELLED,

	/** 거절: 권한 있는 사람이 신청을 거절했다 */
	REJECTED,

	/** 만료: 대기 중인 신청이 확정되지 못하고 끝났다 */
	EXPIRED,

	/** 회차 취소: 회차가 취소되어 진행 중이던 신청이 함께 끝났다 */
	MEETUP_CANCELLED
}
