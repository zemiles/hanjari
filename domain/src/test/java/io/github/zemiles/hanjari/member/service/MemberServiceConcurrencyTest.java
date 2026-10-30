package io.github.zemiles.hanjari.member.service;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.zemiles.hanjari.common.config.ClockConfig;
import io.github.zemiles.hanjari.common.config.JpaConfig;
import io.github.zemiles.hanjari.common.exception.DbConstraints;
import io.github.zemiles.hanjari.common.exception.ErrorType;
import io.github.zemiles.hanjari.common.exception.HanjariException;
import io.github.zemiles.hanjari.member.entity.Member;
import io.github.zemiles.hanjari.member.entity.MemberStatus;
import io.github.zemiles.hanjari.member.repository.MemberRepository;
import io.github.zemiles.hanjari.support.TestcontainersConfiguration;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED) // 테스트 트랜잭션을 끈다: 각 스레드가 직접 커밋해야 서로의 데이터를 본다
@DisplayName("MemberService 동시 가입 테스트")
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class, MemberService.class})
public class MemberServiceConcurrencyTest {

	private final MemberService memberService;

	private final MemberRepository memberRepository;

	private final JdbcTemplate jdbcTemplate;

	@Autowired
	public MemberServiceConcurrencyTest(MemberService memberService, MemberRepository memberRepository,
		JdbcTemplate jdbcTemplate) {
		this.memberService = memberService;
		this.memberRepository = memberRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	// 테스트 트랜잭션이 없어서 롤백되지 않는다. 테스트가 끝날 때마다 직접 지운다
	@AfterEach
	void cleanUp() {
		memberRepository.deleteAllInBatch();
	}

	@RepeatedTest(5)
	@DisplayName("다른 두 사람이 같은 닉네임으로 동시에 가입하면 한 명만 성공하고 나머지는 MEMBER_NICKNAME_DUPLICATED")
	void sameNicknameAtSameTime() throws InterruptedException {
		List<Throwable> failures = runAtSameTime(
			() -> memberService.signup(1L, "홍길동"),
			() -> memberService.signup(2L, "홍길동"));

		assertThat(failures).hasSize(1);
		assertErrorType(failures.get(0), ErrorType.MEMBER_NICKNAME_DUPLICATED);
		assertThat(memberRepository.count()).isEqualTo(1);
	}

	@RepeatedTest(5)
	@DisplayName("같은 카카오 계정으로 동시에 두 번 가입하면 한 번만 성공하고 나머지는 MEMBER_ALREADY_REGISTERED")
	void sameKakaoAccountAtSameTime() throws InterruptedException {
		// 닉네임을 다르게 넣어서 닉네임 제약이 아니라 카카오 회원번호 쪽이 걸리게 한다
		List<Throwable> failures = runAtSameTime(
			() -> memberService.signup(1L, "홍길동"),
			() -> memberService.signup(1L, "김철수"));

		assertThat(failures).hasSize(1);
		assertErrorType(failures.get(0), ErrorType.MEMBER_ALREADY_REGISTERED);
		assertThat(memberRepository.count()).isEqualTo(1);
	}

	@RepeatedTest(5)
	@DisplayName("탈퇴 회원이 서로 다른 닉네임으로 동시에 재가입하면 한 번만 성공하고 닉네임을 덮어쓰지 않는다")
	void withdrawnAccountRejoinsAtSameTime() throws InterruptedException {
		Member withdrawn = memberRepository.saveAndFlush(Member.join(1L, "이전이름"));
		jdbcTemplate.update("update member set status = 'WITHDRAWN', withdrawn_at = now() where id = ?",
			withdrawn.getId());
		ConcurrentLinkedQueue<String> successfulNicknames = new ConcurrentLinkedQueue<>();

		List<Throwable> failures = runAtSameTime(
			() -> successfulNicknames.add(memberService.signup(1L, "홍길동").getNickname()),
			() -> successfulNicknames.add(memberService.signup(1L, "김철수").getNickname()));

		assertThat(failures).hasSize(1);
		assertErrorType(failures.get(0), ErrorType.MEMBER_ALREADY_REGISTERED);
		assertThat(successfulNicknames).hasSize(1);
		assertThat(memberRepository.count()).isEqualTo(1);
		Member rejoined = memberRepository.findByKakaoId(1L).orElseThrow();
		assertThat(rejoined.getId()).isEqualTo(withdrawn.getId());
		assertThat(rejoined.getStatus()).isEqualTo(MemberStatus.ACTIVE);
		assertThat(rejoined.getWithdrawnAt()).isNull();
		assertThat(rejoined.getNickname()).isEqualTo(successfulNicknames.element());
	}

	// 작업들을 동시에 출발시키고, 실패한 작업의 예외만 모아서 돌려준다
	private List<Throwable> runAtSameTime(Callable<?>... tasks) throws InterruptedException {
		CountDownLatch ready = new CountDownLatch(tasks.length); // 모든 스레드가 출발선에 섰는지
		CountDownLatch start = new CountDownLatch(1); // 출발 신호
		List<Future<?>> futures = new ArrayList<>();

		try (ExecutorService executor = Executors.newFixedThreadPool(tasks.length)) {
			for (Callable<?> task : tasks) {
				futures.add(executor.submit(() -> {
					ready.countDown();
					start.await();
					return task.call();
				}));
			}
			ready.await();
			start.countDown();
		} // try 블록이 끝날 때 모든 작업이 끝날 때까지 기다린다

		List<Throwable> failures = new ArrayList<>();
		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (ExecutionException exception) {
				failures.add(exception.getCause());
			}
		}
		return failures;
	}

	// 사전 확인에서 걸리면 HanjariException, 정말 동시에 들어와 DB 제약에서 걸리면 DataIntegrityViolationException이 온다.
	// 어느 쪽이든 같은 ErrorType으로 응답하는지 확인 (DB 제약 쪽은 GlobalExceptionHandler가 DbConstraints로 바꾼다)
	private void assertErrorType(Throwable failure, ErrorType expected) {
		if (failure instanceof DataIntegrityViolationException violation) {
			assertThat(DbConstraints.errorTypeOf(violation)).contains(expected);
			return;
		}
		assertThat(failure).isInstanceOfSatisfying(HanjariException.class,
			exception -> assertThat(exception.getErrorType()).isEqualTo(expected));
	}
}
