package io.github.zemiles.hanjari.club.entity;

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
@Getter
@Table(name = "club_membership_permission")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubMembershipPermission extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "club_membership_id", nullable = false)
	private ClubMembership clubMembership;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Permission permission;

	public ClubMembershipPermission(ClubMembership clubMembership, Permission permission) {
		this.clubMembership = clubMembership;
		this.permission = permission;
	}

}
