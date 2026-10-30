package io.github.zemiles.hanjari.common.exception;

import static org.assertj.core.api.Assertions.*;

import java.sql.SQLException;
import java.util.Map;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandlerTest.TestController.class) // 테스트 클래스 안의 클래스는 컴포넌트 스캔에서 빠져서 직접 등록한다
@DisplayName("GlobalExceptionHandler 관련 테스트")
public class GlobalExceptionHandlerTest {

	private final MockMvcTester mvc;

	@Autowired
	public GlobalExceptionHandlerTest(MockMvcTester mvc) {
		this.mvc = mvc;
	}

	@Test
	@DisplayName("HanjariException은 ErrorType의 상태, 코드, 외부 메시지로 응답한다")
	void handleHanjariException() {
		MvcTestResult result = mvc.get().uri("/test/hanjari").exchange();

		assertErrorResponse(result, ErrorType.MEMBER_NOT_FOUND);
		// 응답에는 status, code, message 세 필드만 있다
		assertThat(result).bodyJson().isStrictlyEqualTo("""
			{"status": 404, "code": "1001", "message": "회원 정보를 찾을 수 없습니다."}
			""");
	}

	@Test
	@DisplayName("요청 값 검증에 실패하면 VALID_ERROR로 응답한다")
	void handleValidation() {
		MvcTestResult result = mvc.post().uri("/test/valid")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"name": ""}
				""")
			.exchange();

		assertErrorResponse(result, ErrorType.VALID_ERROR);
	}

	@Test
	@DisplayName("요청 본문이 JSON 형식이 아니면 INVALID_REQUEST_BODY로 응답한다")
	void handleNotReadable() {
		MvcTestResult result = mvc.post().uri("/test/valid")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{")
			.exchange();

		assertErrorResponse(result, ErrorType.INVALID_REQUEST_BODY);
	}

	@Test
	@DisplayName("없는 주소로 요청하면 RESOURCE_NOT_FOUND로 응답한다")
	void handleNotFound() {
		MvcTestResult result = mvc.get().uri("/test/none").exchange();

		assertErrorResponse(result, ErrorType.RESOURCE_NOT_FOUND);
	}

	@Test
	@DisplayName("지원하지 않는 HTTP 메서드로 요청하면 METHOD_NOT_ALLOWED로 응답한다")
	void handleMethodNotAllowed() {
		MvcTestResult result = mvc.delete().uri("/test/hanjari").exchange();

		assertErrorResponse(result, ErrorType.METHOD_NOT_ALLOWED);
	}

	@Test
	@DisplayName("처리하지 못한 예외는 SYSTEM_ERROR로 응답하고 내부 내용은 응답에 넣지 않는다")
	void handleUnexpected() {
		MvcTestResult result = mvc.get().uri("/test/unexpected").exchange();

		assertErrorResponse(result, ErrorType.SYSTEM_ERROR);
		assertThat(result).bodyText().doesNotContain("내부 정보");
	}

	@Test
	@DisplayName("표에 있는 DB 제약 위반은 그 ErrorType으로 응답하고 제약 이름은 응답에 넣지 않는다")
	void handleMappedConstraintViolation() {
		MvcTestResult result = mvc.get().uri("/test/constraint/mapped").exchange();

		assertErrorResponse(result, ErrorType.MEMBER_NICKNAME_DUPLICATED);
		assertThat(result).bodyText().doesNotContain("uk_member_nickname_active");
	}

	@Test
	@DisplayName("표에 없는 DB 제약 위반은 DB_ERROR로 응답하고 제약 이름은 응답에 넣지 않는다")
	void handleUnmappedConstraintViolation() {
		MvcTestResult result = mvc.get().uri("/test/constraint/unmapped").exchange();

		assertErrorResponse(result, ErrorType.DB_ERROR);
		assertThat(result).bodyText().doesNotContain("uk_unknown");
	}

	// 응답 상태와 JSON의 status, code, message가 ErrorType과 같은지 본다
	private void assertErrorResponse(MvcTestResult result, ErrorType errorType) {
		assertThat(result).hasStatus(errorType.getStatus());
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(errorType.getStatus());
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(errorType.getCode());
		assertThat(result).bodyJson().extractingPath("$.message").isEqualTo(errorType.getExternalMessage());
	}

	// 테스트에서만 쓰는 컨트롤러: 주소마다 정해진 예외를 던진다
	@RestController
	static class TestController {

		@GetMapping("/test/hanjari")
		void hanjari() {
			throw new HanjariException(ErrorType.MEMBER_NOT_FOUND, Map.of("memberId", 1L));
		}

		@PostMapping("/test/valid")
		void valid(@Valid @RequestBody NameRequest request) {
			// 검증만 확인하므로 본문은 비워 둔다
		}

		@GetMapping("/test/unexpected")
		void unexpected() {
			throw new IllegalStateException("내부 정보");
		}

		@GetMapping("/test/constraint/mapped")
		void mappedConstraint() {
			throw constraintViolation("uk_member_nickname_active");
		}

		@GetMapping("/test/constraint/unmapped")
		void unmappedConstraint() {
			throw constraintViolation("uk_unknown");
		}

		// 스프링이 DB 제약 위반을 바꿔 주는 모양 그대로 만든다: 원인에 Hibernate 예외와 제약 이름이 있다
		private DataIntegrityViolationException constraintViolation(String constraintName) {
			String message = "duplicate key value violates unique constraint \"" + constraintName + "\"";
			ConstraintViolationException cause = new ConstraintViolationException(
				message, new SQLException(message, "23505"), constraintName);
			return new DataIntegrityViolationException(message, cause);
		}
	}

	record NameRequest(@NotBlank String name) {
	}

}
