package io.github.zemiles.hanjari.meetup.entity;

/**
 * 확정자가 빠졌을 때 대기자를 확정으로 올리는 방식. 회차마다 정한다.
 */
public enum PromotionMode {

	/** 자동 승격: 대기 순번대로 같은 트랜잭션 안에서 바로 확정한다 */
	AUTO,

	/** 수동 승격: 권한 있는 사람이 대기자를 골라 확정한다. 정한 시간 안에 처리하지 않으면 자동으로 바뀐다 */
	MANUAL
}
