package io.github.zemiles.hanjari.auth.kakao;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.net.SocketTimeoutException;
import java.util.Map;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import io.github.zemiles.hanjari.common.exception.ErrorType;
import io.github.zemiles.hanjari.common.exception.HanjariException;

@RestClientTest(KakaoClient.class)
@DisplayName("KakaoClient 관련 테스트")
@Import(KakaoConfig.class) // KakaoProperties 등록. 값은 test/resources/application.properties의 가짜 값
public class KakaoClientTest {

	private final KakaoClient kakaoClient;
	private final MockRestServiceServer kakaoServer; // 가짜 카카오

	@Autowired
	public KakaoClientTest(KakaoClient kakaoClient, MockRestServiceServer kakaoServer) {
		this.kakaoClient = kakaoClient;
		this.kakaoServer = kakaoServer;
	}

	@Test
	@DisplayName("인가 코드로 토큰을 받고, 그 토큰으로 회원번호와 닉네임을 가져온다")
	void getUser() {
		// 준비: 1번째 요청(토큰)이 이런 모양이어야 하고, 오면 이렇게 응답한다
		kakaoServer.expect(requestTo(KakaoClient.TOKEN_URL))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().formDataContains(Map.of(
				"grant_type", "authorization_code",
				"client_id", "test-client-id",
				"client_secret", "test-client-secret",
				"redirect_uri", "http://localhost:5173/auth/kakao/callback",
				"code", "good-code")))
			.andRespond(withSuccess("""
				{"access_token": "kakao-access-token", "token_type": "bearer", "expires_in": 21599}
				""", MediaType.APPLICATION_JSON));
		// 준비: 2번째 요청(사용자 정보)은 받은 토큰을 헤더에 넣어야 한다
		kakaoServer.expect(requestTo(KakaoClient.USER_URL))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer kakao-access-token"))
			.andRespond(withSuccess("""
				{"id": 1234567890, "kakao_account": {"profile": {"nickname": "홍길동"}}}
				""", MediaType.APPLICATION_JSON));

		// 실행
		KakaoUser user = kakaoClient.getUser("good-code");

		// 확인
		assertThat(user).isEqualTo(new KakaoUser(1234567890L, "홍길동"));
		kakaoServer.verify(); // 위에서 정한 두 요청이 실제로 다 갔는지
	}

	@Test
	@DisplayName("닉네임에 동의하지 않았으면 닉네임은 null이다")
	void getUserWithoutNickname() {
		expectTokenSuccess();
		kakaoServer.expect(requestTo(KakaoClient.USER_URL))
			.andRespond(withSuccess("""
				{"id": 1234567890, "kakao_account": {"profile_nickname_needs_agreement": true}}
				""", MediaType.APPLICATION_JSON));

		KakaoUser user = kakaoClient.getUser("good-code");

		assertThat(user.kakaoId()).isEqualTo(1234567890L);
		assertThat(user.nickname()).isNull();
	}

	@Test
	@DisplayName("잘못됐거나 이미 쓴 인가 코드면 KAKAO_INVALID_CODE")
	void invalidCode() {
		kakaoServer.expect(requestTo(KakaoClient.TOKEN_URL))
			.andRespond(withBadRequest()
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
					{
						"error": "invalid_grant",
						"error_description": "authorization code not found for code=used-code",
						"error_code": "KOE320"
					}
					"""));

		assertErrorType(() -> kakaoClient.getUser("used-code"), ErrorType.KAKAO_INVALID_CODE);
	}

	@Test
	@DisplayName("카카오 서버 오류면 KAKAO_LOGIN_FAILED")
	void kakaoServerError() {
		kakaoServer.expect(requestTo(KakaoClient.TOKEN_URL))
			.andRespond(withServerError());

		assertErrorType(() -> kakaoClient.getUser("good-code"), ErrorType.KAKAO_LOGIN_FAILED);
	}

	@Test
	@DisplayName("카카오가 응답하지 않으면(타임아웃) KAKAO_LOGIN_FAILED")
	void kakaoTimeout() {
		kakaoServer.expect(requestTo(KakaoClient.TOKEN_URL))
			.andRespond(withException(new SocketTimeoutException("Read timed out")));

		assertErrorType(() -> kakaoClient.getUser("good-code"), ErrorType.KAKAO_LOGIN_FAILED);
	}

	@Test
	@DisplayName("토큰은 받았는데 사용자 정보 조회가 실패하면 KAKAO_LOGIN_FAILED")
	void userInfoFailed() {
		expectTokenSuccess();
		kakaoServer.expect(requestTo(KakaoClient.USER_URL))
			.andRespond(withUnauthorizedRequest());

		assertErrorType(() -> kakaoClient.getUser("good-code"), ErrorType.KAKAO_LOGIN_FAILED);
	}

	// 토큰 요청은 성공한다고 가정할 때 쓰는 준비
	private void expectTokenSuccess() {
		kakaoServer.expect(requestTo(KakaoClient.TOKEN_URL))
			.andRespond(withSuccess("""
				{"access_token": "kakao-access-token"}
				""", MediaType.APPLICATION_JSON));
	}

	// HanjariException이 나고, 그 ErrorType이 기대한 값인지 확인
	private void assertErrorType(ThrowingCallable call, ErrorType expected) {
		assertThatThrownBy(call)
			.isInstanceOfSatisfying(HanjariException.class,
				exception -> assertThat(exception.getErrorType()).isEqualTo(expected));
	}

}
