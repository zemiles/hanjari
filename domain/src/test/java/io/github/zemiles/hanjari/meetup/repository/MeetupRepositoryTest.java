package io.github.zemiles.hanjari.meetup.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

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
import io.github.zemiles.hanjari.meetup.entity.MeetupStatus;
import io.github.zemiles.hanjari.meetup.entity.PromotionMode;
import io.github.zemiles.hanjari.support.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;

@DataJpaTest
@DisplayName("MeetupRepository 관련 테스트")
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
public class MeetupRepositoryTest {

	private static final Instant APPLY_OPENS_AT = Instant.parse("2026-10-28T01:00:00Z");

	private static final Instant APPLY_CLOSES_AT = Instant.parse("2026-10-29T01:00:00Z");

	private static final Instant STARTS_AT = Instant.parse("2026-10-30T10:00:00Z");

	private final MeetupRepository meetupRepository;

	private final ClubRepository clubRepository;

	private final EntityManager entityManager;

	@Autowired
	public MeetupRepositoryTest(
		MeetupRepository meetupRepository,
		ClubRepository clubRepository,
		EntityManager entityManager) {
		this.meetupRepository = meetupRepository;
		this.clubRepository = clubRepository;
		this.entityManager = entityManager;
	}

	@Test
	@DisplayName("meetup 생성")
	void createMeetup() {
		Club club = saveClub();

		Meetup saved = meetupRepository.save(newMeetup(club.getId()));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("meetup 조회")
	void findMeetup() {
		Club club = saveClub();
		Meetup meetup = meetupRepository.save(newMeetup(club.getId()));
		entityManager.flush();
		entityManager.clear();

		Meetup result = meetupRepository.findById(meetup.getId()).orElseThrow(
			() -> new HanjariException(ErrorType.MEETUP_NOT_FOUND, Map.of("meetupId", meetup.getId()))
		);

		assertThat(result.getClubId()).isEqualTo(club.getId());
		assertThat(result.getTitle()).isEqualTo("10월 정기전");
		assertThat(result.getPlace()).isEqualTo("강남 볼링장");
		assertThat(result.getStartsAt()).isEqualTo(STARTS_AT);
		assertThat(result.getCapacity()).isEqualTo(10);
		assertThat(result.getApplyOpensAt()).isEqualTo(APPLY_OPENS_AT);
		assertThat(result.getApplyClosesAt()).isEqualTo(APPLY_CLOSES_AT);
		assertThat(result.isWaitlistEnabled()).isTrue();
		assertThat(result.getWaitlistCapacity()).isEqualTo(5);
		assertThat(result.getPromotionMode()).isEqualTo(PromotionMode.MANUAL);
		assertThat(result.getManualFallbackMinutes()).isEqualTo(30);
		assertThat(result.getAttendanceWindowMinutes()).isEqualTo(30);
		assertThat(result.getQrRadiusMeters()).isEqualTo(200);
		assertThat(result.getStatus()).isEqualTo(MeetupStatus.SCHEDULED);
		assertThat(result.getCancelledAt()).isNull();
		assertThat(result.getAttendanceOpenedAt()).isNull();
		// 카운터는 updatable = false라서 처음 저장할 때 넣은 값이 그대로 들어갔는지 본다
		assertThat(result.getConfirmedCount()).isZero();
		assertThat(result.getWaitingCount()).isZero();
		assertThat(result.getNextWaitingSeq()).isEqualTo(1);
	}

	private Club saveClub() {
		return clubRepository.save(new Club("클럽테스트", "테스트 클럽입니다.", Visibility.PUBLIC_LINK,
			PenaltyScope.MEETUP, SuccessionMode.AUTO));
	}

	private Meetup newMeetup(Long clubId) {
		return Meetup.builder()
			.clubId(clubId)
			.title("10월 정기전")
			.place("강남 볼링장")
			.startsAt(STARTS_AT)
			.capacity(10)
			.applyOpensAt(APPLY_OPENS_AT)
			.applyClosesAt(APPLY_CLOSES_AT)
			.waitlistEnabled(true)
			.waitlistCapacity(5)
			.promotionMode(PromotionMode.MANUAL)
			.manualFallbackMinutes(30)
			.attendanceWindowMinutes(30)
			.qrRadiusMeters(200)
			.build();
	}

}
