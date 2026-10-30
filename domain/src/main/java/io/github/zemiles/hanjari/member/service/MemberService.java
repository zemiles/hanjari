package io.github.zemiles.hanjari.member.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.zemiles.hanjari.common.exception.ErrorType;
import io.github.zemiles.hanjari.common.exception.HanjariException;
import io.github.zemiles.hanjari.member.entity.Member;
import io.github.zemiles.hanjari.member.entity.MemberStatus;
import io.github.zemiles.hanjari.member.repository.MemberRepository;

@Service
public class MemberService {

	private final MemberRepository memberRepository;

	public MemberService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	/** 카카오 회원번호로 가입 여부를 확인한다. 활동 회원이면 LoggedIn, 아니면 SignupRequired (회원을 만들거나 바꾸지 않는다) */
	@Transactional(readOnly = true)
	public LoginCheckResult checkLogin(Long kakaoId, String kakaoNickname) {
		Optional<Member> byKakaoId = memberRepository.findByKakaoId(kakaoId);

		if (byKakaoId.isPresent() && byKakaoId.get().getStatus() == MemberStatus.ACTIVE) {
			return new LoginCheckResult.LoggedIn(byKakaoId.get());
		}

		return new LoginCheckResult.SignupRequired(kakaoNickname);
	}

	/** 닉네임을 받아 가입을 마친다. 탈퇴 회원이면 같은 줄을 되살린다 */
	@Transactional
	public Member signup(Long kakaoId, String nickname) {

		Optional<Member> byKakaoId = memberRepository.findForSignupByKakaoId(kakaoId);

		if (byKakaoId.isPresent() && byKakaoId.get().getStatus() == MemberStatus.ACTIVE) {
			throw new HanjariException(ErrorType.MEMBER_ALREADY_REGISTERED,
				Map.of("memberId", byKakaoId.get().getId()));
		}

		String normalized = NicknameRule.normalize(nickname);

		if (memberRepository.existsByNicknameAndStatus(normalized, MemberStatus.ACTIVE)) {
			throw new HanjariException(ErrorType.MEMBER_NICKNAME_DUPLICATED, Map.of());
		}

		Member member;
		if (byKakaoId.isPresent()) {
			// 탈퇴회원 재가입
			member = byKakaoId.get();
			member.rejoin(normalized);
		} else {
			member = Member.join(kakaoId, normalized);
		}

		return memberRepository.saveAndFlush(member);
	}
}
