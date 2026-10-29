package io.github.zemiles.hanjari.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.zemiles.hanjari.member.entity.Member;
import io.github.zemiles.hanjari.member.entity.MemberStatus;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

	/** 카카오 회원번호로 회원을 찾는다 */
	Optional<Member> findByKakaoId(Long kakaoId);

	/** 이 상태(예: ACTIVE)의 회원 중 같은 닉네임이 있는지 */
	boolean existsByNicknameAndStatus(String nickname, MemberStatus status);

	List<Member> findByNickname(String nickname);
}
