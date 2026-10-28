package io.github.zemiles.hanjari.club.entity;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ClubMembership 관련 테스트")
public class ClubMembershipTest {

	private static final Instant NOW = Instant.parse("2026-10-28T01:00:00Z");

	private final Club club = new Club("클럽테스트", "테스트 클럽입니다.", Visibility.PUBLIC_LINK,
		PenaltyScope.MEETUP, SuccessionMode.AUTO);

	@Test
	@DisplayName("클럽장 소속은 활동 상태로 만들고 받은 시각을 활동 시작 시각으로 넣는다")
	void createOwner() {
		ClubMembership owner = ClubMembership.owner(club, 1L, NOW);

		assertThat(owner.getRole()).isEqualTo(MembershipRole.OWNER);
		assertThat(owner.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(owner.getJoinedAt()).isEqualTo(NOW); // 넘긴 시각이 그대로 들어갔는지
	}

	@Test
	@DisplayName("가입 신청 소속은 대기 상태로 만들고 활동 시작 시각은 비워 둔다")
	void createRequest() {
		ClubMembership request = ClubMembership.request(club, 1L);

		assertThat(request.getRole()).isEqualTo(MembershipRole.MEMBER);
		assertThat(request.getStatus()).isEqualTo(MembershipStatus.PENDING);
		assertThat(request.getJoinedAt()).isNull();
	}
}
