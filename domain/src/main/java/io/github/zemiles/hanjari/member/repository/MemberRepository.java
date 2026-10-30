package io.github.zemiles.hanjari.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.zemiles.hanjari.member.entity.Member;
import io.github.zemiles.hanjari.member.entity.MemberStatus;
import jakarta.persistence.LockModeType;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

	/** 카카오 회원번호로 회원을 찾는다 */
	Optional<Member> findByKakaoId(Long kakaoId);

	/** 기존 탈퇴 회원의 동시 재가입을 직렬화한다. 없는 행의 신규 가입 경합은 유니크 제약으로 막는다 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select member from Member member where member.kakaoId = :kakaoId")
	Optional<Member> findForSignupByKakaoId(@Param("kakaoId") Long kakaoId);

	/** 이 상태(예: ACTIVE)의 회원 중 같은 닉네임이 있는지 */
	boolean existsByNicknameAndStatus(String nickname, MemberStatus status);
}
