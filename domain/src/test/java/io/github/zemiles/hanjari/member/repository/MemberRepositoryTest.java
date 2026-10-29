package io.github.zemiles.hanjari.member.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import io.github.zemiles.hanjari.common.config.ClockConfig;
import io.github.zemiles.hanjari.common.config.JpaConfig;
import io.github.zemiles.hanjari.common.exception.ErrorType;
import io.github.zemiles.hanjari.common.exception.HanjariException;
import io.github.zemiles.hanjari.member.entity.Member;
import io.github.zemiles.hanjari.support.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;

@DataJpaTest
@DisplayName("MemberRepository 관련 테스트")
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
public class MemberRepositoryTest {

	private final MemberRepository memberRepository;

	private final EntityManager entityManager;

	@Autowired
	public MemberRepositoryTest(MemberRepository memberRepository, EntityManager entityManager) {
		this.memberRepository = memberRepository;
		this.entityManager = entityManager;
	}

	@Test
	@DisplayName("멤버 생성")
	void createMember() {
		Member member = Member.join(1L, "이순신");
		Member saved = memberRepository.save(member);

		entityManager.flush();
		entityManager.clear();

		assertThat(saved.getId()).isNotNull(); // id가 null 인지
		assertThat(saved.getCreatedAt()).isNotNull(); // 생성 일시가 null 인지
		assertThat(saved.getUpdatedAt()).isNotNull(); // 업데이트 일시가 null인지
	}

	@Test
	@DisplayName("멤버 조회")
	void findMember() {
		Member member = Member.join(1L, "이순신");
		memberRepository.save(member);

		entityManager.flush();
		entityManager.clear();

		Member result = memberRepository.findById(member.getId()).orElseThrow(
			() -> new HanjariException(ErrorType.MEMBER_NOT_FOUND, Map.of("memberId", member.getId()))
		);

		assertThat(result.getId()).isNotNull();
		assertThat(result.getId()).isEqualTo(member.getId());
	}

	@Test
	@DisplayName("활동 회원끼리 같은 닉네임이면 uk_member_nickname_active로 막힌다")
	void duplicateNicknameAmongActiveMembers() {
		memberRepository.saveAndFlush(Member.join(1L, "이순신"));

		assertThatThrownBy(() -> memberRepository.saveAndFlush(Member.join(2L, "이순신")))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("uk_member_nickname_active");
	}

	@Test
	@DisplayName("탈퇴 회원이 쓰던 닉네임은 다른 회원이 쓸 수 있다")
	void nicknameOfWithdrawnMemberIsReusable() {
		Member withdrawn = memberRepository.saveAndFlush(Member.join(1L, "이순신"));
		// Member에 아직 탈퇴 메서드가 없어서 SQL로 탈퇴 상태로 바꾼다
		entityManager.createNativeQuery(
				"UPDATE member SET status = 'WITHDRAWN', withdrawn_at = now() WHERE id = :id")
			.setParameter("id", withdrawn.getId())
			.executeUpdate();

		assertThatCode(() -> memberRepository.saveAndFlush(Member.join(2L, "이순신")))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("영문 대소문자만 다른 닉네임은 다른 닉네임으로 본다")
	void nicknameIsCaseSensitive() {
		memberRepository.saveAndFlush(Member.join(1L, "Abc"));

		assertThatCode(() -> memberRepository.saveAndFlush(Member.join(2L, "abc")))
			.doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(strings = {"이 순신", "이\t순신"})
	@DisplayName("닉네임에 공백이 있으면 ck_member_nickname_no_space로 막힌다")
	void nicknameWithSpaceIsRejected(String nickname) {
		assertThatThrownBy(() -> memberRepository.saveAndFlush(Member.join(1L, nickname)))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("ck_member_nickname_no_space");
	}

}
