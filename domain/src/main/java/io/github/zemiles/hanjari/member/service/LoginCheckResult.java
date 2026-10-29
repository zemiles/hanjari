package io.github.zemiles.hanjari.member.service;

import io.github.zemiles.hanjari.member.entity.Member;

/** 카카오 로그인 확인 결과: 이미 가입한 활동 회원이면 LoggedIn, 아니면 SignupRequired */
public sealed interface LoginCheckResult {

	/** 활동 중인 회원 → 바로 로그인 */
	record LoggedIn(Member member) implements LoginCheckResult {
	}

	/**
	 * 처음 온 사람·탈퇴 회원 → 닉네임 설정 화면으로.
	 * kakaoNickname은 카카오가 준 값 그대로(화면에 미리 채울 값, 동의 안 했으면 null). 검증은 가입할 때 한다
	 */
	record SignupRequired(String kakaoNickname) implements LoginCheckResult {
	}

}
