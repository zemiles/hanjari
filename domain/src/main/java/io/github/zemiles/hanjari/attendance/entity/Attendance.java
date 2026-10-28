package io.github.zemiles.hanjari.attendance.entity;

import java.time.Instant;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attendance")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long applyId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AttendanceStatus status;

	@Column
	private Instant checkedAt;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private CheckMethod checkMethod;

	@Column
	private Long processedBy;

	@Column
	private Integer distanceMeters;

	private Attendance(Long applyId, AttendanceStatus status) {
		this.applyId = applyId;
		this.status = status;
	}

	public static Attendance selfCheck(Long applyId, Instant startsAt, Instant checkedAt, Integer distanceMeters) {
		Attendance attendance = new Attendance(applyId, statusOf(startsAt, checkedAt));
		attendance.checkedAt = checkedAt;
		attendance.checkMethod = CheckMethod.SELF;
		attendance.distanceMeters = distanceMeters;
		return attendance;
	}

	public static Attendance staffCheck(Long applyId, Long processedBy, Instant startsAt, Instant checkedAt) {
		Attendance attendance = new Attendance(applyId, statusOf(startsAt, checkedAt));
		attendance.checkedAt = checkedAt;
		attendance.checkMethod = CheckMethod.STAFF;
		attendance.processedBy = processedBy;
		return attendance;
	}

	public static Attendance noShow(Long applyId, Long processedBy) {
		Attendance attendance = new Attendance(applyId, AttendanceStatus.NO_SHOW);
		attendance.processedBy = processedBy;
		return attendance;
	}

	private static AttendanceStatus statusOf(Instant startAt, Instant checkedAt) {
		return checkedAt.isAfter(startAt) ? AttendanceStatus.LATE : AttendanceStatus.ATTENDED;
	}

}
