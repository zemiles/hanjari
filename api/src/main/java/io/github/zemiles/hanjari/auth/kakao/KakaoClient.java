package io.github.zemiles.hanjari.auth.kakao;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import io.github.zemiles.hanjari.auth.kakao.dto.KakaoTokenResponse;
import io.github.zemiles.hanjari.auth.kakao.dto.KakaoUserResponse;
import io.github.zemiles.hanjari.common.exception.ErrorType;
import io.github.zemiles.hanjari.common.exception.HanjariException;

@Component
public class KakaoClient {

	static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
	static final String USER_URL = "https://kapi.kakao.com/v2/user/me";

	private final RestClient restClient;
	private final KakaoProperties properties;

	public KakaoClient(RestClient.Builder builder, KakaoProperties properties) {
		this.restClient = builder.build();
		this.properties = properties;
	}

	/** 인가 코드 → 카카오 토큰 → 카카오 사용자 정보. 카카오 토큰은 여기서 쓰고 버린다 */
	public KakaoUser getUser(String code) {
		String accessToken = requestAccessToken(code);
		KakaoUserResponse user = requestUser(accessToken);
		return new KakaoUser(user.id(), user.nickname());
	}

	// 1번째 호출: 인가 코드를 카카오 토큰으로 바꾼다
	private String requestAccessToken(String code) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", properties.clientId());
		form.add("client_secret", properties.clientSecret());
		form.add("redirect_uri", properties.redirectUri());
		form.add("code", code);

		try {
			KakaoTokenResponse response = restClient.post()
				.uri(TOKEN_URL)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(KakaoTokenResponse.class);
			if (response == null || response.accessToken() == null) {
				throw failed(null);
			}
			return response.accessToken();
		} catch (HttpClientErrorException.BadRequest exception) {
			// 잘못됐거나 이미 쓴 인가 코드: 카카오가 400 + "invalid_grant"를 준다
			if (exception.getResponseBodyAsString().contains("\"invalid_grant\"")) {
				throw new HanjariException(ErrorType.KAKAO_INVALID_CODE, Map.of(), exception);
			}
			throw failed(exception);
		} catch (RestClientException exception) {
			// 카카오 4xx·5xx, 타임아웃, 응답을 못 읽음
			throw failed(exception);
		}
	}

	// 2번째 호출: 카카오 토큰으로 사용자 정보를 조회한다
	private KakaoUserResponse requestUser(String accessToken) {
		try {
			KakaoUserResponse response = restClient.get()
				.uri(USER_URL)
				.headers(headers -> headers.setBearerAuth(accessToken))
				.retrieve()
				.body(KakaoUserResponse.class);
			if (response == null || response.id() == null) {
				throw failed(null);
			}
			return response;
		} catch (RestClientException exception) {
			throw failed(exception);
		}
	}

	private HanjariException failed(Throwable cause) {
		return new HanjariException(ErrorType.KAKAO_LOGIN_FAILED, Map.of(), cause);
	}

}
