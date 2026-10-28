package io.github.zemiles.hanjari.club.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import io.github.zemiles.hanjari.club.entity.Club;
import io.github.zemiles.hanjari.club.entity.ClubMembership;
import io.github.zemiles.hanjari.club.entity.ClubMembershipPermission;
import io.github.zemiles.hanjari.club.entity.PenaltyScope;
import io.github.zemiles.hanjari.club.entity.Permission;
import io.github.zemiles.hanjari.club.entity.SuccessionMode;
import io.github.zemiles.hanjari.club.entity.Visibility;
import io.github.zemiles.hanjari.common.config.ClockConfig;
import io.github.zemiles.hanjari.common.config.JpaConfig;
import io.github.zemiles.hanjari.member.entity.Member;
import io.github.zemiles.hanjari.member.repository.MemberRepository;
import io.github.zemiles.hanjari.support.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;

@DataJpaTest
@DisplayName("ClubMembershipPermissionRepository 관련 테스트")
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
public class ClubMembershipPermissionRepositoryTest {

	private static final Instant NOW = Instant.parse("2026-10-28T01:00:00Z");

	private final ClubMembershipRepository clubMembershipRepository;

	private final ClubRepository clubRepository;

	private final MemberRepository memberRepository;

	private final ClubMembershipPermissionRepository clubMembershipPermissionRepository;

	private final EntityManager entityManager;

	@Autowired
	public ClubMembershipPermissionRepositoryTest(
		ClubRepository clubRepository,
		MemberRepository memberRepository,
		ClubMembershipRepository clubMembershipRepository,
		ClubMembershipPermissionRepository clubMembershipPermissionRepository,
		EntityManager entityManager) {
		this.clubRepository = clubRepository;
		this.memberRepository = memberRepository;
		this.clubMembershipRepository = clubMembershipRepository;
		this.clubMembershipPermissionRepository = clubMembershipPermissionRepository;
		this.entityManager = entityManager;
	}

	@Test
	@DisplayName("ClubMembershipPermission 생성")
	void createClubMembershipPermission() {
		ClubMembership clubMembership = saveClubMembership();

		ClubMembershipPermission saved = clubMembershipPermissionRepository.save(
			new ClubMembershipPermission(clubMembership, Permission.MEETUP_MANAGE));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("권한 4개를 모두 저장하고 조회")
	void saveAndFindAllPermissions() {
		ClubMembership clubMembership = saveClubMembership();
		for (Permission permission : Permission.values()) {
			clubMembershipPermissionRepository.save(new ClubMembershipPermission(clubMembership, permission));
		}
		entityManager.flush();
		entityManager.clear();

		List<ClubMembershipPermission> result = clubMembershipPermissionRepository.findAll();

		assertThat(result)
			.extracting(ClubMembershipPermission::getPermission)
			.containsExactlyInAnyOrder(Permission.values()); // 4개 권한이 빠짐없이 저장됐는지
		assertThat(result)
			.extracting(row -> row.getClubMembership().getId())
			.containsOnly(clubMembership.getId()); // 모두 같은 소속에 붙었는지
	}

	// 운영진으로 바꾸는 메서드가 아직 없어서 소속은 클럽장으로 만든다
	private ClubMembership saveClubMembership() {
		Member member = memberRepository.save(new Member(1L, "테스트1"));
		Club club = clubRepository.save(
			new Club("클럽테스트", "테스트 클럽입니다.", Visibility.PUBLIC_LINK, PenaltyScope.CLUB, SuccessionMode.AUTO));
		return clubMembershipRepository.save(ClubMembership.owner(club, member.getId(), NOW));
	}

}
