package io.github.zemiles.hanjari.common.exception;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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
	}

	record NameRequest(@NotBlank String name) {
	}

}
