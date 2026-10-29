package io.github.zemiles.hanjari.member.service;

import static org.assertj.core.api.Assertions.*;

import java.text.Normalizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.zemiles.hanjari.common.exception.ErrorType;
import io.github.zemiles.hanjari.common.exception.HanjariException;

@DisplayName("NicknameRule 관련 테스트")
public class NicknameRuleTest {

	@Test
	@DisplayName("앞뒤 공백은 잘라낸다")
	void trimsLeadingAndTrailingSpaces() {
		assertThat(NicknameRule.normalize("  이순신  ")).isEqualTo("이순신");
	}

	@Test
	@DisplayName("NFC로 정규화한다 (자모가 분리된 한글도 정상 한글로 받는다)")
	void normalizesToNfc() {
		String decomposed = Normalizer.normalize("한자리", Normalizer.Form.NFD); // 자모가 분리된 형태
		assertThat(NicknameRule.normalize(decomposed)).isEqualTo("한자리");
	}

	@ParameterizedTest
	@ValueSource(strings = {"가나", "한자리2Go", "abcDEF123", "가가가가가가가가가가"})
	@DisplayName("한글·영문·숫자로 된 2~10자는 그대로 허용한다")
	void allowsValidNickname(String input) {
		assertThat(NicknameRule.normalize(input)).isEqualTo(input);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"   ", "\t"})
	@DisplayName("비어 있거나 공백뿐이면 VALID_ERROR")
	void rejectsBlank(String input) {
		assertValidError(input);
	}

	@ParameterizedTest
	@ValueSource(strings = {"가", "가가가가가가가가가가가"})
	@DisplayName("1자이거나 10자를 넘으면 VALID_ERROR")
	void rejectsInvalidLength(String input) {
		assertValidError(input);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"이 순신", // 가운데 공백
		"이\u00A0순신", // 줄 안 바꿈 공백
		"이순\u200B신", // 폭 없는 공백 (눈에 안 보임)
		"이순신😀", // 이모지
		"이순신!", // 특수문자
		"ㅋㅋ", // 자음만
		"홍길동ㅋ", // 완성형 + 자음
		"\u0430dmin", // 러시아 문자 а (영어 a와 생김새가 같음)
		"漢字" // 한자
	})
	@DisplayName("한글 완성형·영문·숫자가 아닌 글자가 있으면 VALID_ERROR")
	void rejectsNotAllowedCharacters(String input) {
		assertValidError(input);
	}

	// HanjariException이 나고, 그 ErrorType이 VALID_ERROR인지 확인
	private void assertValidError(String input) {
		assertThatThrownBy(() -> NicknameRule.normalize(input))
			.isInstanceOfSatisfying(HanjariException.class,
				exception -> assertThat(exception.getErrorType()).isEqualTo(ErrorType.VALID_ERROR));
	}

}
