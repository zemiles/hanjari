package io.github.zemiles.hanjari.club.entity;

import java.time.Instant;

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
@Table(name = "club_membership")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubMembership extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "club_id", nullable = false)
	private Club club;

	@Column(nullable = false)
	private Long memberId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MembershipRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MembershipStatus status;

	@Column
	private Instant joinedAt;

	private ClubMembership(Club club, Long memberId, MembershipRole role, MembershipStatus status,
		Instant joinedAt) {
		this.club = club;
		this.memberId = memberId;
		this.role = role;
		this.status = status;
		this.joinedAt = joinedAt;
	}

}
