package io.github.zemiles.hanjari.auth.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserResponse(
	Long id,
	@JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record KakaoAccount(Profile profile) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Profile(String nickname) {
	}

	/** 닉네임 동의를 안 했으면 profile이 없으므로 null을 돌려준다 */
	public String nickname() {
		if (kakaoAccount == null || kakaoAccount.profile() == null) {
			return null;
		}
		return kakaoAccount.profile().nickname();
	}
}
