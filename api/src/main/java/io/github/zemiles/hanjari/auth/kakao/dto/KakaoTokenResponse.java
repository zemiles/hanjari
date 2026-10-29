package io.github.zemiles.hanjari.auth.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jspecify.annotations.NonNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoTokenResponse(
	@JsonProperty("access_token") String accessToken
) {
	@Override
	public @NonNull String toString() {
		return "KakaoTokenResponse[accessToken=***]";
	}
}
