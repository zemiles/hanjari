package io.github.zemiles.hanjari.club.entity;

/**
 * 노쇼 패널티를 받은 회원을 어디까지 막을지. 클럽을 만들 때 고르고, 기본은 MEETUP이다.
 */
public enum PenaltyScope {

	/** 회차 단위 차단: 그 클럽의 회차 신청만 막는다. 클럽 소속은 유지된다 */
	MEETUP,

	/** 클럽 단위 차단: 그 클럽의 가입과 회차 신청을 모두 막는다 */
	CLUB
}
