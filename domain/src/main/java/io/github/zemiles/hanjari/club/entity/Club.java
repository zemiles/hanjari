package io.github.zemiles.hanjari.club.entity;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "club")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Club extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(length = 500)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Visibility visibility;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PenaltyScope penaltyScope;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SuccessionMode successionMode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ClubStatus status;

	@Column
	private Instant closesAt;

	@Builder
	private Club(String name, String description, Visibility visibility, PenaltyScope penaltyScope,
		SuccessionMode successionMode) {
		this.name = name;
		this.description = description;
		this.visibility = visibility;
		this.penaltyScope = penaltyScope;
		this.successionMode = successionMode;
		this.status = ClubStatus.ACTIVE;
	}
}
