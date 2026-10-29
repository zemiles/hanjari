package io.github.zemiles.hanjari.member.entity;

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
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long kakaoId;

	@Column(nullable = false, length = 30)
	private String nickname;

	@Column(length = 255)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MemberStatus status;

	@Column
	private Instant lastSeenAt;

	@Column
	private Instant withdrawnAt;

	private Member(Long kakaoId, String nickname) {
		this.kakaoId = kakaoId;
		this.nickname = nickname;
		this.status = MemberStatus.ACTIVE;
	}

	/** 카카오 계정으로 처음 가입한다. 닉네임은 NicknameRule로 검사한 값을 받는다 */
	public static Member join(Long kakaoId, String nickname) {
		return new Member(kakaoId, nickname);
	}

	/**
	 * 탈퇴한 회원이 같은 카카오 계정으로 다시 가입한다. 새 줄을 만들지 않고 이 줄을 되살린다 (2026-10-26 결정).
	 * 닉네임은 NicknameRule로 검사한 값을 받는다. 노쇼 패널티는 그대로 남고, 클럽은 다시 가입 신청해야 한다
	 */
	public void rejoin(String nickname) {
		if (status != MemberStatus.WITHDRAWN) {
			// service가 탈퇴 회원일 때만 부르므로, 여기 오면 코드 실수다
			throw new IllegalStateException("탈퇴한 회원만 다시 가입할 수 있다. memberId=" + id + ", status=" + status);
		}
		this.status = MemberStatus.ACTIVE;
		this.withdrawnAt = null;
		this.nickname = nickname;
	}
}
