package io.github.zemiles.hanjari.auth.kakao;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "kakao")
public record KakaoProperties(
	@NotBlank String clientId,
	@NotBlank String clientSecret,
	@NotBlank String redirectUri
) {
	@Override
	public @NonNull String toString() {
		return "KakaoProperties[clientId=" + clientId + ", redirectUri=" + redirectUri + ", clientSecret=***]";
	}
}
