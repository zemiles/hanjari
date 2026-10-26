package io.github.zemiles.hanjari.apply.entity;

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
@Table(name = "apply")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Apply extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long meetupId;

	@Column(nullable = false)
	private Long memberId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ApplyStatus status;

	@Column
	private Integer waitingSeq;

	@Column(nullable = false, length = 64)
	private String idempotencyKey;

	@Column(nullable = false)
	private Instant appliedAt;

	@Column
	private Instant confirmedAt;

	@Column
	private Instant endedAt;

	private Apply(Long meetupId, Long memberId, String idempotencyKey, Instant appliedAt) {
		this.meetupId = meetupId;
		this.memberId = memberId;
		this.idempotencyKey = idempotencyKey;
		this.appliedAt = appliedAt;
	}
}
