package io.github.zemiles.hanjari.member.dto;

import io.github.zemiles.hanjari.member.entity.Member;

public record MemberResponse(Long id, String nickname) {
	public static MemberResponse from(Member member) {
		return new MemberResponse(member.getId(), member.getNickname());
	}
}
