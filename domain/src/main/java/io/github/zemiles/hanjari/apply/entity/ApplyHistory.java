package io.github.zemiles.hanjari.apply.entity;

import io.github.zemiles.hanjari.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "apply_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplyHistory extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "apply_id", nullable = false)
	private Apply apply;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private ApplyStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ApplyStatus toStatus;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ActorType actorType;

	@Column
	private Long actorMemberId;

	private ApplyHistory(Apply apply, ApplyStatus fromStatus, ApplyStatus toStatus, ActorType actorType,
		Long actorMemberId) {
		this.apply = apply;
		this.fromStatus = fromStatus;
		this.toStatus = toStatus;
		this.actorType = actorType;
		this.actorMemberId = actorMemberId;
	}



}
