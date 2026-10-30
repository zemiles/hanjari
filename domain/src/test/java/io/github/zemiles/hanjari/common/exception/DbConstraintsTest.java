package io.github.zemiles.hanjari.common.exception;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import io.github.zemiles.hanjari.common.config.ClockConfig;
import io.github.zemiles.hanjari.common.config.JpaConfig;
import io.github.zemiles.hanjari.member.entity.Member;
import io.github.zemiles.hanjari.member.repository.MemberRepository;
import io.github.zemiles.hanjari.support.TestcontainersConfiguration;

@DataJpaTest
@DisplayName("DbConstraints 관련 테스트")
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
public class DbConstraintsTest {

	private final MemberRepository memberRepository;

	private final JdbcTemplate jdbcTemplate;

	@Autowired
	public DbConstraintsTest(MemberRepository memberRepository, JdbcTemplate jdbcTemplate) {
		this.memberRepository = memberRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Test
	@DisplayName("표의 모든 제약 이름이 Flyway가 만든 실제 유니크 인덱스에 존재한다")
	void mappedNamesExistInDatabase() {
		List<String> uniqueIndexes = jdbcTemplate.queryForList("""
			select indexname from pg_indexes
			where schemaname = current_schema() and indexdef like 'CREATE UNIQUE INDEX %'
			""", String.class);

		assertThat(uniqueIndexes).containsAll(DbConstraints.mappedNames());
	}

	@Test
	@DisplayName("카카오 회원번호가 겹치면 uk_member_kakao_id를 MEMBER_ALREADY_REGISTERED로 바꾼다")
	void kakaoIdDuplicated() {
		memberRepository.saveAndFlush(Member.join(1L, "이순신"));

		DataIntegrityViolationException exception = violationOf(
			() -> memberRepository.saveAndFlush(Member.join(1L, "홍길동")));

		assertThat(DbConstraints.violatedName(exception)).isEqualTo("uk_member_kakao_id");
		assertThat(DbConstraints.errorTypeOf(exception)).contains(ErrorType.MEMBER_ALREADY_REGISTERED);
	}

	@Test
	@DisplayName("활동 회원끼리 닉네임이 겹치면 uk_member_nickname_active를 MEMBER_NICKNAME_DUPLICATED로 바꾼다")
	void nicknameDuplicated() {
		memberRepository.saveAndFlush(Member.join(1L, "이순신"));

		DataIntegrityViolationException exception = violationOf(
			() -> memberRepository.saveAndFlush(Member.join(2L, "이순신")));

		assertThat(DbConstraints.violatedName(exception)).isEqualTo("uk_member_nickname_active");
		assertThat(DbConstraints.errorTypeOf(exception)).contains(ErrorType.MEMBER_NICKNAME_DUPLICATED);
	}

	@Test
	@DisplayName("표에 없는 제약이면 이름은 알려 주고 ErrorType은 비어 있다")
	void constraintNotInTable() {
		DataIntegrityViolationException exception = violationOf(
			() -> memberRepository.saveAndFlush(Member.join(1L, "이 순신")));

		assertThat(DbConstraints.violatedName(exception)).isEqualTo("ck_member_nickname_no_space");
		assertThat(DbConstraints.errorTypeOf(exception)).isEmpty();
	}

	@Test
	@DisplayName("원인이 제약 위반이 아니면 이름은 null이고 ErrorType은 비어 있다")
	void causeIsNotConstraintViolation() {
		DataIntegrityViolationException exception = new DataIntegrityViolationException("제약 정보 없음");

		assertThat(DbConstraints.violatedName(exception)).isNull();
		assertThat(DbConstraints.errorTypeOf(exception)).isEmpty();
	}

	// 실행했을 때 DataIntegrityViolationException이 나는지 확인하고 그 예외를 돌려준다
	private DataIntegrityViolationException violationOf(ThrowingCallable callable) {
		Throwable thrown = catchThrowable(callable);
		assertThat(thrown).isInstanceOf(DataIntegrityViolationException.class);
		return (DataIntegrityViolationException) thrown;
	}

}
