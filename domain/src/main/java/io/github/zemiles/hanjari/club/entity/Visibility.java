package io.github.zemiles.hanjari.club.entity;

/**
 * 클럽의 공개 방식. 누가 회차에 신청할 수 있는지 정한다.
 */
public enum Visibility {

	/** 링크 공개: 링크를 가진 사람 누구나 가입 승인 없이 신청한다 */
	PUBLIC_LINK,

	/** 멤버 전용: 가입 신청 후 승인받은 멤버만 신청한다 */
	MEMBERS_ONLY
}
