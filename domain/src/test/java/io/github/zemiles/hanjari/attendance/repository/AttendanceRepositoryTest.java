package io.github.zemiles.hanjari.attendance.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import io.github.zemiles.hanjari.apply.entity.Apply;
import io.github.zemiles.hanjari.apply.repository.ApplyRepository;
import io.github.zemiles.hanjari.attendance.entity.Attendance;
import io.github.zemiles.hanjari.attendance.entity.AttendanceStatus;
import io.github.zemiles.hanjari.attendance.entity.CheckMethod;
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
@DisplayName("AttendanceRepository 관련 테스트")
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
public class AttendanceRepositoryTest {

	private static final Instant NOW = Instant.parse("2026-10-28T01:00:00Z");

	private static final Instant STARTS_AT = Instant.parse("2026-10-30T10:00:00Z");

	private static final Instant BEFORE_START = Instant.parse("2026-10-30T09:50:00Z");

	private static final Instant AFTER_START = Instant.parse("2026-10-30T10:10:00Z");

	private final AttendanceRepository attendanceRepository;

	private final ApplyRepository applyRepository;

	private final MeetupRepository meetupRepository;

	private final ClubRepository clubRepository;

	private final MemberRepository memberRepository;

	private final EntityManager entityManager;

	@Autowired
	public AttendanceRepositoryTest(
		AttendanceRepository attendanceRepository,
		ApplyRepository applyRepository,
		MeetupRepository meetupRepository,
		ClubRepository clubRepository,
		MemberRepository memberRepository,
		EntityManager entityManager) {
		this.attendanceRepository = attendanceRepository;
		this.applyRepository = applyRepository;
		this.meetupRepository = meetupRepository;
		this.clubRepository = clubRepository;
		this.memberRepository = memberRepository;
		this.entityManager = entityManager;
	}

	@Test
	@DisplayName("Attendance 생성")
	void createAttendance() {
		Apply apply = saveApply();

		Attendance saved = attendanceRepository.save(Attendance.selfCheck(apply.getId(), STARTS_AT, BEFORE_START, 30));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("셀프 체크한 출석 조회")
	void findSelfCheckAttendance() {
		Apply apply = saveApply();
		Attendance attendance = attendanceRepository.save(
			Attendance.selfCheck(apply.getId(), STARTS_AT, BEFORE_START, 30));
		entityManager.flush();
		entityManager.clear();

		Attendance result = attendanceRepository.findById(attendance.getId()).orElseThrow();

		assertThat(result.getApplyId()).isEqualTo(apply.getId());
		assertThat(result.getStatus()).isEqualTo(AttendanceStatus.ATTENDED);
		assertThat(result.getCheckedAt()).isEqualTo(BEFORE_START);
		assertThat(result.getCheckMethod()).isEqualTo(CheckMethod.SELF);
		assertThat(result.getProcessedBy()).isNull();
		assertThat(result.getDistanceMeters()).isEqualTo(30);
	}

	@Test
	@DisplayName("대신 처리한 지각 조회")
	void findStaffCheckAttendance() {
		Member staff = memberRepository.save(new Member(2L, "운영진"));
		Apply apply = saveApply();
		Attendance attendance = attendanceRepository.save(
			Attendance.staffCheck(apply.getId(), staff.getId(), STARTS_AT, AFTER_START));
		entityManager.flush();
		entityManager.clear();

		Attendance result = attendanceRepository.findById(attendance.getId()).orElseThrow();

		assertThat(result.getStatus()).isEqualTo(AttendanceStatus.LATE);
		assertThat(result.getCheckedAt()).isEqualTo(AFTER_START);
		assertThat(result.getCheckMethod()).isEqualTo(CheckMethod.STAFF);
		assertThat(result.getProcessedBy()).isEqualTo(staff.getId());
		assertThat(result.getDistanceMeters()).isNull();
	}

	@Test
	@DisplayName("노쇼 조회")
	void findNoShowAttendance() {
		Member staff = memberRepository.save(new Member(2L, "운영진"));
		Apply apply = saveApply();
		Attendance attendance = attendanceRepository.save(Attendance.noShow(apply.getId(), staff.getId()));
		entityManager.flush();
		entityManager.clear();

		Attendance result = attendanceRepository.findById(attendance.getId()).orElseThrow();

		assertThat(result.getStatus()).isEqualTo(AttendanceStatus.NO_SHOW);
		assertThat(result.getCheckedAt()).isNull(); // 노쇼는 체크한 적이 없다
		assertThat(result.getCheckMethod()).isNull();
		assertThat(result.getProcessedBy()).isEqualTo(staff.getId()); // 노쇼를 확정한 사람
		assertThat(result.getDistanceMeters()).isNull();
	}

	private Apply saveApply() {
		Member member = memberRepository.save(new Member(1L, "신청자"));
		Club club = clubRepository.save(new Club("클럽테스트", "테스트 클럽입니다.", Visibility.PUBLIC_LINK,
			PenaltyScope.MEETUP, SuccessionMode.AUTO));
		Meetup meetup = meetupRepository.save(Meetup.builder()
			.clubId(club.getId())
			.title("10월 정기전")
			.startsAt(STARTS_AT)
			.capacity(10)
			.applyOpensAt(NOW)
			.applyClosesAt(Instant.parse("2026-10-29T01:00:00Z"))
			.promotionMode(PromotionMode.AUTO)
			.build());
		return applyRepository.save(Apply.createConfirmed(meetup.getId(), member.getId(), "apply-key-1", NOW));
	}

}
