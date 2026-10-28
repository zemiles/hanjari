package io.github.zemiles.hanjari.club.entity;

/**
 * 클럽장 자리가 비었을 때 채우는 방식. 클럽을 만들 때 클럽장이 고른다.
 */
public enum SuccessionMode {

	/** 자동 승계: 부클럽장 → 조건을 통과한 운영진 → 운영진 투표 → 클럽 투표 순으로 채운다 */
	AUTO,

	/** 투표: 처음부터 클럽 투표로 뽑는다 */
	VOTE
}
