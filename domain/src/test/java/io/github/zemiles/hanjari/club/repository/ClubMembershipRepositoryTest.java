package io.github.zemiles.hanjari.club.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

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
		Member member = Member.join(1L, "테스트1");
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
		Member member = Member.join(1L, "테스트1");
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

	@Test
	@DisplayName("한 클럽에 클럽장이 이미 있으면 두 번째 클럽장은 거부된다")
	void rejectSecondOwner() {
		Member owner = memberRepository.save(Member.join(1L, "클럽장"));
		Member otherMember = memberRepository.save(Member.join(2L, "다른회원"));
		clubRepository.save(club);
		clubMembershipRepository.saveAndFlush(ClubMembership.owner(club, owner.getId(), NOW));

		// 다른 회원으로 넣어서 uk_club_membership_owner만 걸리게 한다
		ClubMembership secondOwner = ClubMembership.owner(club, otherMember.getId(), NOW);

		assertThatThrownBy(() -> clubMembershipRepository.saveAndFlush(secondOwner))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("uk_club_membership_owner");
	}

	@Test
	@DisplayName("클럽장이 있는 클럽에도 클럽장이 아닌 소속은 저장된다")
	void allowMemberInClubWithOwner() {
		Member owner = memberRepository.save(Member.join(1L, "클럽장"));
		Member otherMember = memberRepository.save(Member.join(2L, "다른회원"));
		clubRepository.save(club);
		clubMembershipRepository.saveAndFlush(ClubMembership.owner(club, owner.getId(), NOW));

		// uk_club_membership_owner는 클럽장 줄에만 걸리는 부분 유니크 인덱스다
		ClubMembership saved = clubMembershipRepository.saveAndFlush(ClubMembership.request(club, otherMember.getId()));

		assertThat(saved.getId()).isNotNull();
	}
}
