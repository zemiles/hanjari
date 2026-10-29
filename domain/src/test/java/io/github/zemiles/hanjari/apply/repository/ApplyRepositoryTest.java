package io.github.zemiles.hanjari.apply.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import io.github.zemiles.hanjari.apply.entity.Apply;
import io.github.zemiles.hanjari.apply.entity.ApplyStatus;
import io.github.zemiles.hanjari.club.entity.Club;
import io.github.zemiles.hanjari.club.entity.PenaltyScope;
import io.github.zemiles.hanjari.club.entity.SuccessionMode;
import io.github.zemiles.hanjari.club.entity.Visibility;
import io.github.zemiles.hanjari.club.repository.ClubRepository;
import io.github.zemiles.hanjari.common.config.ClockConfig;
import io.github.zemiles.hanjari.common.config.JpaConfig;
import io.github.zemiles.hanjari.common.exception.ErrorType;
import io.github.zemiles.hanjari.common.exception.HanjariException;
import io.github.zemiles.hanjari.meetup.entity.Meetup;
import io.github.zemiles.hanjari.meetup.entity.PromotionMode;
import io.github.zemiles.hanjari.meetup.repository.MeetupRepository;
import io.github.zemiles.hanjari.member.entity.Member;
import io.github.zemiles.hanjari.member.repository.MemberRepository;
import io.github.zemiles.hanjari.support.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;

@DataJpaTest
@DisplayName("ApplyRepository 관련 테스트")
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
public class ApplyRepositoryTest {

	private static final Instant NOW = Instant.parse("2026-10-28T01:00:00Z");

	private static final String IDEMPOTENCY_KEY = "apply-key-1";

	private final ApplyRepository applyRepository;

	private final MeetupRepository meetupRepository;

	private final ClubRepository clubRepository;

	private final MemberRepository memberRepository;

	private final EntityManager entityManager;

	@Autowired
	public ApplyRepositoryTest(
		ApplyRepository applyRepository,
		MeetupRepository meetupRepository,
		ClubRepository clubRepository,
		MemberRepository memberRepository,
		EntityManager entityManager) {
		this.applyRepository = applyRepository;
		this.meetupRepository = meetupRepository;
		this.clubRepository = clubRepository;
		this.memberRepository = memberRepository;
		this.entityManager = entityManager;
	}

	@Test
	@DisplayName("apply 생성")
	void createApply() {
		Meetup meetup = saveMeetup();
		Member member = saveMember();

		Apply saved = applyRepository.save(
			Apply.createConfirmed(meetup.getId(), member.getId(), IDEMPOTENCY_KEY, NOW));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("확정 신청 조회")
	void findConfirmedApply() {
		Meetup meetup = saveMeetup();
		Member member = saveMember();
		Apply apply = applyRepository.save(
			Apply.createConfirmed(meetup.getId(), member.getId(), IDEMPOTENCY_KEY, NOW));
		entityManager.flush();
		entityManager.clear();

		Apply result = getApply(apply.getId());

		assertThat(result.getMeetupId()).isEqualTo(meetup.getId());
		assertThat(result.getMemberId()).isEqualTo(member.getId());
		assertThat(result.getStatus()).isEqualTo(ApplyStatus.CONFIRMED);
		assertThat(result.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
		assertThat(result.getAppliedAt()).isEqualTo(NOW);
		assertThat(result.getConfirmedAt()).isEqualTo(NOW);
		assertThat(result.getWaitingSeq()).isNull();
		assertThat(result.getEndedAt()).isNull();
	}

	@Test
	@DisplayName("대기 신청 조회")
	void findWaitingApply() {
		Meetup meetup = saveMeetup();
		Member member = saveMember();
		Apply apply = applyRepository.save(
			Apply.createWaiting(meetup.getId(), member.getId(), IDEMPOTENCY_KEY, 1, NOW));
		entityManager.flush();
		entityManager.clear();

		Apply result = getApply(apply.getId());

		assertThat(result.getStatus()).isEqualTo(ApplyStatus.WAITING);
		assertThat(result.getWaitingSeq()).isEqualTo(1);
		assertThat(result.getAppliedAt()).isEqualTo(NOW);
		assertThat(result.getConfirmedAt()).isNull();
		assertThat(result.getEndedAt()).isNull();
	}

	@Test
	@DisplayName("같은 회차에 진행 중인 신청이 있으면 같은 회원의 두 번째 신청은 거부된다")
	void rejectSecondActiveApply() {
		Meetup meetup = saveMeetup();
		Member member = saveMember();
		applyRepository.saveAndFlush(Apply.createConfirmed(meetup.getId(), member.getId(), IDEMPOTENCY_KEY, NOW));

		// 멱등키는 다르게 넣어서 uk_apply_active만 걸리게 한다
		Apply second = Apply.createWaiting(meetup.getId(), member.getId(), "apply-key-2", 1, NOW);

		assertThatThrownBy(() -> applyRepository.saveAndFlush(second))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("uk_apply_active");
	}

	@Test
	@DisplayName("같은 멱등키로 신청하면 두 번째 신청은 거부된다")
	void rejectDuplicateIdempotencyKey() {
		Meetup meetup = saveMeetup();
		Member member = saveMember();
		Member otherMember = memberRepository.save(new Member(2L, "다른신청자"));
		applyRepository.saveAndFlush(Apply.createConfirmed(meetup.getId(), member.getId(), IDEMPOTENCY_KEY, NOW));

		// 회원을 다르게 넣어서 uk_apply_idempotency_key만 걸리게 한다
		Apply second = Apply.createConfirmed(meetup.getId(), otherMember.getId(), IDEMPOTENCY_KEY, NOW);

		assertThatThrownBy(() -> applyRepository.saveAndFlush(second))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("uk_apply_idempotency_key");
	}

	private Apply getApply(Long applyId) {
		return applyRepository.findById(applyId).orElseThrow(
			() -> new HanjariException(ErrorType.APPLY_NOT_FOUND, Map.of("applyId", applyId))
		);
	}

	private Member saveMember() {
		return memberRepository.save(new Member(1L, "신청자"));
	}

	private Meetup saveMeetup() {
		Club club = clubRepository.save(new Club("클럽테스트", "테스트 클럽입니다.", Visibility.PUBLIC_LINK,
			PenaltyScope.MEETUP, SuccessionMode.AUTO));
		return meetupRepository.save(Meetup.builder()
			.clubId(club.getId())
			.title("10월 정기전")
			.startsAt(Instant.parse("2026-10-30T10:00:00Z"))
			.capacity(10)
			.applyOpensAt(NOW)
			.applyClosesAt(Instant.parse("2026-10-29T01:00:00Z"))
			.promotionMode(PromotionMode.AUTO)
			.build());
	}

}
