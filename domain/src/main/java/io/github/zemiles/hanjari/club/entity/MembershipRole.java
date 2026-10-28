package io.github.zemiles.hanjari.club.entity;

/**
 * 클럽 안에서의 역할. 이 이름들은 club_membership.role에서만 쓴다.
 */
public enum MembershipRole {

	/** 클럽장: 모든 권한을 가진다. 클럽마다 1명 */
	OWNER,

	/** 부클럽장: 네 권한을 고정으로 가진다. 클럽마다 최대 1명 */
	VICE_OWNER,

	/** 운영진: 클럽장이 사람마다 권한을 켜고 끈다 */
	MANAGER,

	/** 클럽원 */
	MEMBER
}
