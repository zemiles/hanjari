package io.github.zemiles.hanjari.member.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

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
		Member member = new Member(1L, "이순신");
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
		Member member = new Member(1L, "이순신");
		memberRepository.save(member);

		entityManager.flush();
		entityManager.clear();

		Member result = memberRepository.findById(member.getId()).orElseThrow(
			() -> new HanjariException(ErrorType.MEMBER_NOT_FOUND, Map.of("memberId", member.getId()))
		);

		assertThat(result.getId()).isNotNull();
		assertThat(result.getId()).isEqualTo(member.getId());
	}

}
