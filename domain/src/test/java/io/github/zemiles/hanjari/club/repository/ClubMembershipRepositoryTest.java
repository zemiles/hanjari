package io.github.zemiles.hanjari.club.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import io.github.zemiles.hanjari.club.entity.Club;
import io.github.zemiles.hanjari.club.entity.ClubMembership;
import io.github.zemiles.hanjari.club.entity.MembershipRole;
import io.github.zemiles.hanjari.club.entity.MembershipStatus;
import io.github.zemiles.hanjari.club.entity.PenaltyScope;
import io.github.zemiles.hanjari.club.entity.SuccessionMode;
import io.github.zemiles.hanjari.club.entity.Visibility;
import io.github.zemiles.hanjari.common.config.ClockConfig;
import io.github.zemiles.hanjari.common.config.JpaConfig;
import io.github.zemiles.hanjari.common.exception.ErrorType;
import io.github.zemiles.hanjari.common.exception.HanjariException;
import io.github.zemiles.hanjari.member.entity.Member;
import io.github.zemiles.hanjari.member.repository.MemberRepository;
import io.github.zemiles.hanjari.support.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;

@DataJpaTest
@DisplayName("ClubMembershipRepository 관련 테스트")
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
public class ClubMembershipRepositoryTest {

	private static final Instant NOW = Instant.parse("2026-10-28T01:00:00Z");

	private final EntityManager entityManager;

	private final Club club = new Club("클럽테스트", "테스트 클럽입니다.", Visibility.PUBLIC_LINK,
		PenaltyScope.MEETUP, SuccessionMode.AUTO);

	private final ClubMembershipRepository clubMembershipRepository;

	private final ClubRepository clubRepository;

	private final MemberRepository memberRepository;

	@Autowired
	public ClubMembershipRepositoryTest(
		ClubRepository clubRepository,
		MemberRepository memberRepository,
		ClubMembershipRepository clubMembershipRepository,
		EntityManager entityManager) {
		this.clubRepository = clubRepository;
		this.memberRepository = memberRepository;
		this.entityManager = entityManager;
		this.clubMembershipRepository = clubMembershipRepository;
	}

	@Test
	@DisplayName("ClubMembership 생성")
	void createClubMembership() {
		Member member = new Member(1L, "테스트1");
		memberRepository.save(member);
		clubRepository.save(club);

		entityManager.flush();
		entityManager.clear();

		ClubMembership clubMembership = ClubMembership.owner(club, member.getId(), NOW);
		ClubMembership save = clubMembershipRepository.save(clubMembership);

		entityManager.flush();
		entityManager.clear();

		assertThat(save.getId()).isNotNull();
	}

	@Test
	@DisplayName("ClubMembership 조회")
	void findClubMembership() {
		Member member = new Member(1L, "테스트1");
		memberRepository.save(member);
		clubRepository.save(club);

		entityManager.flush();
		entityManager.clear();

		ClubMembership clubMembership = ClubMembership.owner(club, member.getId(), NOW);
		clubMembershipRepository.save(clubMembership);

		entityManager.flush();
		entityManager.clear();

		ClubMembership result = clubMembershipRepository.findById(clubMembership.getId()).orElseThrow(
			() -> new HanjariException(ErrorType.CLUB_MEMBERSHIP_NOT_FOUND,
				Map.of("clubMembershipId", clubMembership.getId()))
		);

		assertThat(result.getId()).isEqualTo(clubMembership.getId());
		assertThat(result.getClub().getId()).isEqualTo(club.getId());
		assertThat(result.getMemberId()).isEqualTo(member.getId());
		assertThat(result.getRole()).isEqualTo(MembershipRole.OWNER);
		assertThat(result.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(result.getJoinedAt()).isEqualTo(NOW); // DB에 다녀와도 넘긴 시각 그대로인지
	}
}
