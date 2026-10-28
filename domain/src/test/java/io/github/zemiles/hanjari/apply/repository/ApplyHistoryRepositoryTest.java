package io.github.zemiles.hanjari.apply.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import io.github.zemiles.hanjari.apply.entity.ActorType;
import io.github.zemiles.hanjari.apply.entity.Apply;
import io.github.zemiles.hanjari.apply.entity.ApplyHistory;
import io.github.zemiles.hanjari.apply.entity.ApplyStatus;
import io.github.zemiles.hanjari.club.entity.Club;
import io.github.zemiles.hanjari.club.entity.PenaltyScope;
import io.github.zemiles.hanjari.club.entity.SuccessionMode;
import io.github.zemiles.hanjari.club.entity.Visibility;
import io.github.zemiles.hanjari.club.repository.ClubRepository;
import io.github.zemiles.hanjari.common.config.ClockConfig;
import io.github.zemiles.hanjari.common.config.JpaConfig;
import io.github.zemiles.hanjari.meetup.entity.Meetup;
import io.github.zemiles.hanjari.meetup.entity.PromotionMode;
import io.github.zemiles.hanjari.meetup.repository.MeetupRepository;
import io.github.zemiles.hanjari.member.entity.Member;
import io.github.zemiles.hanjari.member.repository.MemberRepository;
import io.github.zemiles.hanjari.support.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;

@DataJpaTest
@DisplayName("ApplyHistoryRepository 관련 테스트")
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
public class ApplyHistoryRepositoryTest {

	private static final Instant NOW = Instant.parse("2026-10-28T01:00:00Z");

	private final ApplyHistoryRepository applyHistoryRepository;

	private final ApplyRepository applyRepository;

	private final MeetupRepository meetupRepository;

	private final ClubRepository clubRepository;

	private final MemberRepository memberRepository;

	private final EntityManager entityManager;

	@Autowired
	public ApplyHistoryRepositoryTest(
		ApplyHistoryRepository applyHistoryRepository,
		ApplyRepository applyRepository,
		MeetupRepository meetupRepository,
		ClubRepository clubRepository,
		MemberRepository memberRepository,
		EntityManager entityManager) {
		this.applyHistoryRepository = applyHistoryRepository;
		this.applyRepository = applyRepository;
		this.meetupRepository = meetupRepository;
		this.clubRepository = clubRepository;
		this.memberRepository = memberRepository;
		this.entityManager = entityManager;
	}

	@Test
	@DisplayName("ApplyHistory 생성")
	void createApplyHistory() {
		Apply apply = saveApply();

		ApplyHistory saved = applyHistoryRepository.save(ApplyHistory.bySelf(apply, null, ApplyStatus.CONFIRMED));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("본인이 바꾼 이력 조회")
	void findApplyHistoryBySelf() {
		Apply apply = saveApply();
		// 처음 신청할 때 남기는 이력이라 이전 상태는 비어 있다
		ApplyHistory history = applyHistoryRepository.save(ApplyHistory.bySelf(apply, null, ApplyStatus.CONFIRMED));
		entityManager.flush();
		entityManager.clear();

		ApplyHistory result = applyHistoryRepository.findById(history.getId()).orElseThrow();

		assertThat(result.getApply().getId()).isEqualTo(apply.getId());
		assertThat(result.getFromStatus()).isNull();
		assertThat(result.getToStatus()).isEqualTo(ApplyStatus.CONFIRMED);
		assertThat(result.getActorType()).isEqualTo(ActorType.SELF);
		assertThat(result.getActorMemberId()).isEqualTo(apply.getMemberId()); // 본인은 신청한 사람
	}

	@Test
	@DisplayName("권한 있는 사람이 바꾼 이력 조회")
	void findApplyHistoryByStaff() {
		Member staff = memberRepository.save(new Member(2L, "운영진"));
		Apply apply = saveApply();
		// 수동 승격: 대기 → 확정
		ApplyHistory history = applyHistoryRepository.save(
			ApplyHistory.byStaff(apply, ApplyStatus.WAITING, ApplyStatus.CONFIRMED, staff.getId()));
		entityManager.flush();
		entityManager.clear();

		ApplyHistory result = applyHistoryRepository.findById(history.getId()).orElseThrow();

		assertThat(result.getFromStatus()).isEqualTo(ApplyStatus.WAITING);
		assertThat(result.getToStatus()).isEqualTo(ApplyStatus.CONFIRMED);
		assertThat(result.getActorType()).isEqualTo(ActorType.STAFF);
		assertThat(result.getActorMemberId()).isEqualTo(staff.getId());
	}

	@Test
	@DisplayName("시스템이 바꾼 이력 조회")
	void findApplyHistoryBySystem() {
		Apply apply = saveApply();
		// 자동 승격: 대기 → 확정
		ApplyHistory history = applyHistoryRepository.save(
			ApplyHistory.bySystem(apply, ApplyStatus.WAITING, ApplyStatus.CONFIRMED));
		entityManager.flush();
		entityManager.clear();

		ApplyHistory result = applyHistoryRepository.findById(history.getId()).orElseThrow();

		assertThat(result.getActorType()).isEqualTo(ActorType.SYSTEM);
		assertThat(result.getActorMemberId()).isNull(); // 시스템은 바꾼 사람이 없다
	}

	private Apply saveApply() {
		Member member = memberRepository.save(new Member(1L, "신청자"));
		Club club = clubRepository.save(new Club("클럽테스트", "테스트 클럽입니다.", Visibility.PUBLIC_LINK,
			PenaltyScope.MEETUP, SuccessionMode.AUTO));
		Meetup meetup = meetupRepository.save(Meetup.builder()
			.clubId(club.getId())
			.title("10월 정기전")
			.startsAt(Instant.parse("2026-10-30T10:00:00Z"))
			.capacity(10)
			.applyOpensAt(NOW)
			.applyClosesAt(Instant.parse("2026-10-29T01:00:00Z"))
			.promotionMode(PromotionMode.AUTO)
			.build());
		return applyRepository.save(Apply.createConfirmed(meetup.getId(), member.getId(), "apply-key-1", NOW));
	}

}
