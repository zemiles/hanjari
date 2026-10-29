package io.github.zemiles.hanjari.member.service;

import java.text.Normalizer;
import java.util.Map;
import java.util.regex.Pattern;

import io.github.zemiles.hanjari.common.exception.ErrorType;
import io.github.zemiles.hanjari.common.exception.HanjariException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 닉네임 규칙을 한 곳에 모은다: 한글 완성형·영문·숫자만, 2~10자.
 * 카카오 닉네임은 화면에 미리 채울 뿐이고, 검증은 가입할 때 여기서 한 번만 한다.
 * 욕설·예약어 검사(HJR-92)도 여기에 붙인다
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NicknameRule {

	public static final int MIN_LENGTH = 2;
	public static final int MAX_LENGTH = 10;

	private static final Pattern ALLOWED = Pattern.compile("^[가-힣a-zA-Z0-9]+$");

	/** 사용자가 입력한 닉네임을 검사하고 정리한다. 규칙을 어기면 VALID_ERROR */
	public static String normalize(String input) {
		if (input == null) {
			throw invalid("null");
		}

		String nickname = Normalizer.normalize(input.strip(), Normalizer.Form.NFC);

		if (!ALLOWED.matcher(nickname).matches()) {
			throw invalid("not allowed characters");
		}

		if (nickname.length() < MIN_LENGTH || nickname.length() > MAX_LENGTH) {
			throw invalid("length=" + nickname.length());
		}

		return nickname;
	}

	// 닉네임 값 자체는 로그에 남기지 않고 이유만 남긴다
	private static HanjariException invalid(String reason) {
		return new HanjariException(ErrorType.VALID_ERROR, Map.of("reason", reason));
	}

}
