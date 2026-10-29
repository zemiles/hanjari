package io.github.zemiles.hanjari.member.service;

import static org.assertj.core.api.Assertions.*;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import io.github.zemiles.hanjari.common.config.ClockConfig;
import io.github.zemiles.hanjari.common.config.JpaConfig;
import io.github.zemiles.hanjari.common.exception.ErrorType;
import io.github.zemiles.hanjari.common.exception.HanjariException;
import io.github.zemiles.hanjari.member.entity.Member;
import io.github.zemiles.hanjari.member.entity.MemberStatus;
import io.github.zemiles.hanjari.member.repository.MemberRepository;
import io.github.zemiles.hanjari.support.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;

@DataJpaTest
@DisplayName("MemberService 관련 테스트")
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class, MemberService.class})
public class MemberServiceTest {

	private final MemberService memberService;

	private final MemberRepository memberRepository;

	private final EntityManager entityManager;

	@Autowired
	public MemberServiceTest(MemberService memberService, MemberRepository memberRepository,
		EntityManager entityManager) {
		this.memberService = memberService;
		this.memberRepository = memberRepository;
		this.entityManager = entityManager;
	}

	@Nested
	@DisplayName("checkLogin: 카카오 로그인 직후 회원인지 확인")
	class CheckLogin {

		@Test
		@DisplayName("활동 회원이면 LoggedIn")
		void activeMemberIsLoggedIn() {
			Member member = Member.join(1L, "홍길동");
			memberRepository.saveAndFlush(member);

			entityManager.clear();

			LoginCheckResult result = memberService.checkLogin(1L, "홍길동");

			assertThat(result).isInstanceOfSatisfying(LoginCheckResult.LoggedIn.class,
				loggedIn -> assertThat(loggedIn.member().getId()).isEqualTo(member.getId()));
		}

		@Test
		@DisplayName("처음 온 사람이면 SignupRequired, 카카오 닉네임은 그대로 담긴다")
		void newUserNeedsSignup() {
			LoginCheckResult result = memberService.checkLogin(1L, "홍길동😀");

			assertThat(result).isEqualTo(new LoginCheckResult.SignupRequired("홍길동😀"));
		}

		@Test
		@DisplayName("탈퇴 회원이면 SignupRequired")
		void withdrawnMemberNeedsSignup() {
			// 준비: 활동 회원을 저장하고 withdraw()로 탈퇴 상태로 바꾼다
			Member member = saveActiveMember(1L, "홍길동");
			Assertions.assertNotNull(member);
			withdraw(member);
			// 실행: 그 회원의 카카오 회원번호로 checkLogin
			LoginCheckResult result = memberService.checkLogin(1L, "카카오닉네임");
			// 확인: LoggedIn이 아니라 SignupRequired이고, 카카오 닉네임이 그대로 담긴다
			assertThat(result).isEqualTo(new LoginCheckResult.SignupRequired("카카오닉네임"));
		}

		@Test
		@DisplayName("카카오 닉네임이 없으면 SignupRequired의 닉네임은 null")
		void kakaoNicknameCanBeNull() {
			// 준비: 따로 없다 (DB에 회원이 없다)
			// 실행: 카카오 닉네임을 null로 checkLogin (닉네임 동의를 안 한 경우)
			LoginCheckResult result = memberService.checkLogin(1L, null);
			// 확인: new LoginCheckResult.SignupRequired(null)과 같다
			assertThat(result).isEqualTo(new LoginCheckResult.SignupRequired(null));
		}

		@Test
		@DisplayName("확인만 하고 회원을 만들지 않는다")
		void doesNotCreateMember() {
			// 준비: 따로 없다
			// 실행: 처음 온 사람의 카카오 회원번호로 checkLogin
			memberService.checkLogin(1L, "처음");
			// 확인: memberRepository.count()가 0이다
			assertThat(memberRepository.count()).isZero();
		}

		@Test
		@DisplayName("기존 회원의 닉네임을 카카오 닉네임으로 덮어쓰지 않는다")
		void doesNotOverwriteNickname() {
			// 준비: 활동 회원을 "홍길동"으로 저장
			Member member = saveActiveMember(1L, "홍길동");

			// 실행: 같은 카카오 회원번호, 다른 카카오 닉네임("김철수")으로 checkLogin
			memberService.checkLogin(1L, "김철수");

			// 확인: entityManager.flush()·clear() 뒤 DB에서 다시 조회한 닉네임이 여전히 "홍길동"
			//       (clear를 안 하면 메모리에 있던 객체를 보게 돼서 DB 값을 확인한 게 아니다)
			entityManager.flush();
			entityManager.clear();
			Member found = memberRepository.findById(member.getId()).orElseThrow();
			assertThat(found.getNickname()).isEqualTo("홍길동");
		}
	}

	@Nested
	@DisplayName("signup: 닉네임을 받아 가입 완료")
	class Signup {
		@Test
		@DisplayName("처음 온 사람은 정리된 닉네임(앞뒤 공백 잘림)으로 활동 회원이 된다")
		void createsMember() {
			// 준비: 따로 없다
			// 실행: 앞뒤 공백이 있는 닉네임("  홍길동 ")으로 signup
			Member member = memberService.signup(1L, " 홍길동 ");
			// 확인: 반환된 회원의 id가 있고, 닉네임 "홍길동", 상태 ACTIVE, 카카오 회원번호가 맞다
			//       flush·clear 뒤 DB에서 다시 조회해도 같다
			entityManager.clear();
			Member found = memberRepository.findById(member.getId()).orElseThrow();
			assertThat(found.getKakaoId()).isEqualTo(1L);
			assertThat(found.getNickname()).isEqualTo("홍길동");
			assertThat(found.getStatus()).isEqualTo(MemberStatus.ACTIVE);
		}

		@Test
		@DisplayName("닉네임이 규칙에 맞지 않으면 VALID_ERROR, 회원은 생기지 않는다")
		void rejectsInvalidNickname() {
			// 준비: 따로 없다
			// 실행: 규칙에 안 맞는 닉네임(예: "홍 길동")으로 signup
			assertErrorType(() -> memberService.signup(1L, "홍 길동"), ErrorType.VALID_ERROR);
			// 확인: HanjariException + VALID_ERROR, memberRepository.count()가 0
			assertThat(memberRepository.count()).isZero();
		}

		@Test
		@DisplayName("활동 회원과 같은 닉네임이면 MEMBER_NICKNAME_DUPLICATED")
		void rejectsDuplicateNickname() {
			// 준비: 활동 회원을 "홍길동"으로 저장 (카카오 회원번호 1)
			saveActiveMember(1L, "홍길동");
			// 실행·확인: 다른 카카오 회원번호(2)로 "홍길동" signup → MEMBER_NICKNAME_DUPLICATED
			assertErrorType(() -> memberService.signup(2L, "홍길동"), ErrorType.MEMBER_NICKNAME_DUPLICATED);
		}

		@Test
		@DisplayName("탈퇴 회원이 쓰던 닉네임은 쓸 수 있다")
		void allowsNicknameOfWithdrawnMember() {
			// 준비: 활동 회원을 "홍길동"으로 저장한 뒤 withdraw()로 탈퇴 (카카오 회원번호 1)
			Member withdrawn = saveActiveMember(1L, "홍길동");
			withdraw(withdrawn);

			// 실행: 다른 카카오 회원번호(2)로 "홍길동" signup
			Member member = memberService.signup(2L, "홍길동");

			// 확인: 에러 없이 새 회원으로 가입되고, 닉네임이 "홍길동"
			assertThat(member.getId()).isNotEqualTo(withdrawn.getId());
			assertThat(member.getNickname()).isEqualTo("홍길동");
		}

		@Test
		@DisplayName("탈퇴 회원은 같은 id로 되살아난다 (ACTIVE, 탈퇴 시각 비움, 새 닉네임)")
		void rejoinsWithdrawnMember() {
			// 준비: 활동 회원을 저장(id를 기억)한 뒤 withdraw()로 탈퇴
			Member before = saveActiveMember(1L, "홍길동");
			withdraw(before);

			// 실행: 같은 카카오 회원번호, 새 닉네임으로 signup
			Member member = memberService.signup(1L, "새닉네임");

			// 확인: 반환된 회원의 id가 처음과 같다(새 줄이 아니다), ACTIVE, withdrawnAt null, 닉네임이 새 값
			//       memberRepository.count()가 1
			assertThat(member.getId()).isEqualTo(before.getId());
			entityManager.clear();
			Member found = memberRepository.findById(before.getId()).orElseThrow();
			assertThat(found.getStatus()).isEqualTo(MemberStatus.ACTIVE);
			assertThat(found.getWithdrawnAt()).isNull();
			assertThat(found.getNickname()).isEqualTo("새닉네임");
			assertThat(memberRepository.count()).isEqualTo(1);
		}

		@Test
		@DisplayName("이미 활동 중인 회원이 다시 가입하면 거부한다 (정할 것 1번에 따라 바뀜)")
		void rejectsAlreadyActiveMember() {
			// 준비: 활동 회원 저장
			saveActiveMember(1L, "홍길동");

			// 실행·확인: 같은 카카오 회원번호로 다시 signup → MEMBER_ALREADY_REGISTERED, 회원은 그대로 1명
			assertErrorType(() -> memberService.signup(1L, "김철수"), ErrorType.MEMBER_ALREADY_REGISTERED);
			assertThat(memberRepository.count()).isEqualTo(1);
		}
	}

	// 활동 회원 한 명을 저장한다
	private Member saveActiveMember(Long kakaoId, String nickname) {
		// memberRepository.saveAndFlush(Member.join(kakaoId, nickname))를 돌려준다
		return memberRepository.saveAndFlush(Member.join(kakaoId, nickname));
	}

	// Member에 아직 탈퇴 메서드가 없어서(HJR-13) SQL로 탈퇴 상태로 바꾼다
	private void withdraw(Member member) {
		// 1. entityManager.createNativeQuery(...)로 아래 SQL을 실행한다 (:id에 member.getId())
		//    UPDATE member SET status = 'WITHDRAWN', withdrawn_at = now() WHERE id = :id
		//    상태만 바꾸면 ck_member_withdrawn_at(탈퇴면 탈퇴 시각 필수)에 걸려서 withdrawn_at도 같이 넣는다
		// 2. entityManager.clear()로 메모리(영속성 컨텍스트)를 비운다
		//    SQL은 DB만 바꾸고 메모리의 회원 객체는 ACTIVE 그대로 남는다. 안 비우면 service가 조회할 때
		//    이 오래된 객체를 돌려받아서 탈퇴 회원인데도 ACTIVE로 보인다
		entityManager.flush();
		entityManager.createNativeQuery("UPDATE member SET status = 'WITHDRAWN', withdrawn_at = now() WHERE id = :id")
			.setParameter("id", member.getId())
			.executeUpdate();
		entityManager.clear();
	}

	// HanjariException이 나고, 그 ErrorType이 기대한 값인지 확인
	private void assertErrorType(ThrowableAssert.ThrowingCallable call, ErrorType expected) {
		assertThatThrownBy(call)
			.isInstanceOfSatisfying(HanjariException.class,
				exception -> assertThat(exception.getErrorType()).isEqualTo(expected));
	}

}
