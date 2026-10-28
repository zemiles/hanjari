package io.github.zemiles.hanjari.member.entity;

/**
 * 회원의 상태.
 */
public enum MemberStatus {

	/** 활동 */
	ACTIVE,

	/** 회원 탈퇴: 닉네임과 이메일을 지운다. 6개월 안에 같은 카카오 계정으로 돌아오면 다시 활동으로 되살린다 */
	WITHDRAWN
}
