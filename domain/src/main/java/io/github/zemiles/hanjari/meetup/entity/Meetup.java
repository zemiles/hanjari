package io.github.zemiles.hanjari.meetup.entity;

import java.time.Instant;
import java.util.Objects;

import io.github.zemiles.hanjari.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meetup")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meetup extends BaseTimeEntity {

	private static final int DEFAULT_ATTENDANCE_WINDOW_MINUTES = 60;
	private static final int DEFAULT_QR_RADIUS_METERS = 100;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long clubId;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(length = 200)
	private String place;

	@Column(nullable = false)
	private Instant startsAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MeetupStatus status;

	@Column
	private Instant cancelledAt;

	@Column(nullable = false)
	private int capacity;

	@Column(nullable = false)
	private Instant applyOpensAt;

	@Column(nullable = false)
	private Instant applyClosesAt;

	@Column(nullable = false)
	private boolean waitlistEnabled;

	@Column
	private Integer waitlistCapacity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PromotionMode promotionMode;

	@Column
	private Integer manualFallbackMinutes;

	@Column(nullable = false)
	private int attendanceWindowMinutes;

	@Column(nullable = false)
	private int qrRadiusMeters;

	@Column
	private Instant attendanceOpenedAt;

	// 카운터 3개는 조건부 UPDATE 쿼리로만 바꾼다 (엔티티 저장 때는 건드리지 않음)
	@Column(nullable = false, updatable = false)
	private int confirmedCount;

	@Column(nullable = false, updatable = false)
	private int waitingCount;

	@Column(nullable = false, updatable = false)
	private int nextWaitingSeq;

	@Builder
	private Meetup(Long clubId, String title, String place, Instant startsAt, int capacity,
		Instant applyOpensAt, Instant applyClosesAt, Boolean waitlistEnabled, Integer waitlistCapacity,
		PromotionMode promotionMode, Integer manualFallbackMinutes, Integer attendanceWindowMinutes,
		Integer qrRadiusMeters) {
		this.clubId = clubId;
		this.title = title;
		this.place = place;
		this.startsAt = startsAt;
		this.capacity = capacity;
		this.applyOpensAt = applyOpensAt;
		this.applyClosesAt = applyClosesAt;
		this.waitlistEnabled = Objects.requireNonNullElse(waitlistEnabled, true);
		this.waitlistCapacity = waitlistCapacity;
		this.promotionMode = promotionMode;
		this.manualFallbackMinutes = manualFallbackMinutes;
		this.attendanceWindowMinutes = Objects.requireNonNullElse(attendanceWindowMinutes,
			DEFAULT_ATTENDANCE_WINDOW_MINUTES);
		this.qrRadiusMeters = Objects.requireNonNullElse(qrRadiusMeters, DEFAULT_QR_RADIUS_METERS);
		this.status = MeetupStatus.SCHEDULED;
		this.confirmedCount = 0;
		this.waitingCount = 0;
		this.nextWaitingSeq = 1;
	}
}
