package io.github.zemiles.hanjari.common.exception;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ErrorType 테스트")
public class ErrorTypeTest {

	@Test
	@DisplayName("")
	void codesAreUnique() {
		List<String> codes = Arrays.stream(ErrorType.values())
			.map(ErrorType::getCode)
			.toList();

		assertThat(codes).doesNotHaveDuplicates();
	}

	@Test
	@DisplayName("에러 코드는 네 자리 숫자이고 메시지가 비어 있지 않다")
	void codesAndMessagesAreValid() {
		for (ErrorType errorType : ErrorType.values()) {
			assertThat(errorType.getCode()).matches("[1-9]\\d{3}");
			assertThat(errorType.getInternalMessage()).isNotBlank();
			assertThat(errorType.getExternalMessage()).isNotBlank();
		}
	}

}
