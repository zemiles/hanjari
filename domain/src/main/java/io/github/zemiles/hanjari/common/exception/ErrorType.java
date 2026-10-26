package io.github.zemiles.hanjari.common.exception;

import org.slf4j.event.Level;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorType {

	// 9xxx 시스템·공통
	SYSTEM_ERROR(500, "9001", Level.ERROR, "처리하지 못한 예외", "일시적인 오류입니다. 잠시 후 다시 시도해 주세요."),
	DB_ERROR(500, "9002", Level.ERROR, "DB 처리 실패", "일시적인 오류입니다. 잠시 후 다시 시도해 주세요."),
	VALID_ERROR(400, "9003", Level.INFO, "요청 값 검증 실패", "입력값을 확인해 주세요."),

	// 1xxx 회원
	MEMBER_NOT_FOUND(404, "1001", Level.INFO, "회원 조회 실패", "회원 정보를 찾을 수 없습니다."),
	MEMBER_ALREADY_WITHDRAWN(409, "1002", Level.INFO, "이미 탈퇴한 회원", "이미 탈퇴한 회원입니다."),

	// 2xxx 클럽
	CLUB_NOT_FOUND(404, "2001", Level.INFO, "클럽 조회 실패", "클럽을 찾을 수 없습니다."),
	CLUB_PERMISSION_DENIED(403, "2002", Level.WARN, "클럽 권한 없음", "이 작업을 할 권한이 없습니다."),

	// 4xxx 신청
	APPLY_ALREADY_EXISTS(409, "4001", Level.INFO, "진행 중 신청 중복", "이미 신청한 회차입니다."),
	APPLY_INVALID_TRANSITION(409, "4002", Level.WARN, "허용되지 않은 신청 상태 전이",
		"현재 상태에서는 할 수 없는 요청입니다."),
	;

	private final int status;
	private final String code;
	private final Level logLevel;
	private final String internalMessage; // 로그용
	private final String externalMessage; // 응답용
}
