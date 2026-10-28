package io.github.zemiles.hanjari.common.exception;

import org.slf4j.event.Level;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 서비스에서 내보내는 에러 종류.
 * 코드 번호대: 1xxx 회원, 2xxx 클럽, 3xxx 회차, 4xxx 신청, 5xxx 출석, 6xxx 노쇼 패널티,
 * 7xxx 클럽장 승계·투표, 8xxx 인증, 9xxx 시스템·공통.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorType {

	// 9xxx 시스템·공통
	SYSTEM_ERROR(500, "9001", Level.ERROR, "처리하지 못한 예외", "일시적인 오류입니다. 잠시 후 다시 시도해 주세요."),
	DB_ERROR(500, "9002", Level.ERROR, "DB 처리 실패", "일시적인 오류입니다. 잠시 후 다시 시도해 주세요."),
	VALID_ERROR(400, "9003", Level.INFO, "요청 값 검증 실패", "입력값을 확인해 주세요."),
	INVALID_REQUEST_BODY(400, "9004", Level.INFO, "요청 본문을 읽을 수 없음", "요청 형식이 올바르지 않습니다."),
	RESOURCE_NOT_FOUND(404, "9005", Level.INFO, "존재하지 않는 경로", "요청한 주소를 찾을 수 없습니다."),
	METHOD_NOT_ALLOWED(405, "9006", Level.INFO, "지원하지 않는 HTTP 메서드", "지원하지 않는 요청 방식입니다."),
	TOO_MANY_REQUESTS(429, "9007", Level.WARN, "요청 횟수 제한 초과", "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
	EXTERNAL_API_ERROR(502, "9008", Level.ERROR, "외부 API 호출 실패", "일시적인 오류입니다. 잠시 후 다시 시도해 주세요."),

	// 1xxx 회원
	MEMBER_NOT_FOUND(404, "1001", Level.INFO, "회원 조회 실패", "회원 정보를 찾을 수 없습니다."),
	MEMBER_ALREADY_WITHDRAWN(409, "1002", Level.INFO, "이미 탈퇴한 회원", "이미 탈퇴한 회원입니다."),

	// 2xxx 클럽
	CLUB_NOT_FOUND(404, "2001", Level.INFO, "클럽 조회 실패", "클럽을 찾을 수 없습니다."),
	CLUB_PERMISSION_DENIED(403, "2002", Level.WARN, "클럽 권한 없음", "이 작업을 할 권한이 없습니다."),
	CLUB_CLOSED(409, "2003", Level.INFO, "폐쇄 예고 또는 폐쇄된 클럽에서의 요청", "폐쇄되었거나 폐쇄 예정인 클럽입니다."),
	CLUB_MEMBERSHIP_NOT_FOUND(404, "2004", Level.INFO, "클럽 소속 조회 실패", "클럽 멤버 정보를 찾을 수 없습니다."),
	CLUB_ALREADY_JOINED(409, "2005", Level.INFO, "이미 가입했거나 가입 신청한 클럽", "이미 가입했거나 가입 신청한 클럽입니다."),
	CLUB_MEMBERSHIP_INVALID_TRANSITION(409, "2006", Level.WARN, "허용되지 않은 소속 상태 전이", "현재 상태에서는 할 수 없는 요청입니다."),
	CLUB_OWNER_CANNOT_BE_KICKED(409, "2007", Level.WARN, "클럽장 내보내기 시도", "클럽장은 내보낼 수 없습니다."),
	CLUB_VICE_OWNER_ALREADY_EXISTS(409, "2008", Level.INFO, "부클럽장 중복 지정", "부클럽장은 한 명만 지정할 수 있습니다."),
	CLUB_PERMISSION_TARGET_NOT_MANAGER(409, "2009", Level.INFO, "운영진이 아닌 소속의 권한 변경", "운영진에게만 권한을 설정할 수 있습니다."),
	CLUB_SETTINGS_LOCKED(409, "2010", Level.INFO, "클럽장 공석 중 잠긴 기능 요청", "클럽장이 정해질 때까지 사용할 수 없는 기능입니다."),

	// 3xxx 회차
	MEETUP_NOT_FOUND(404, "3001", Level.INFO, "회차 조회 실패", "회차를 찾을 수 없습니다."),
	MEETUP_ALREADY_CANCELLED(409, "3002", Level.INFO, "이미 취소된 회차", "취소된 회차입니다."),
	MEETUP_ALREADY_STARTED(409, "3003", Level.INFO, "이미 시작한 회차", "이미 시작한 회차입니다."),
	MEETUP_CAPACITY_BELOW_CONFIRMED(409, "3004", Level.INFO, "확정 인원보다 작은 정원으로 변경", "확정된 인원보다 정원을 작게 줄일 수 없습니다."),
	MEETUP_INVALID_PERIOD(400, "3005", Level.INFO, "신청 기간과 시작 시각이 맞지 않음", "신청 기간과 회차 시작 시각을 확인해 주세요."),

	// 4xxx 신청
	APPLY_ALREADY_EXISTS(409, "4001", Level.INFO, "진행 중 신청 중복", "이미 신청한 회차입니다."),
	APPLY_INVALID_TRANSITION(409, "4002", Level.WARN, "허용되지 않은 신청 상태 전이", "현재 상태에서는 할 수 없는 요청입니다."),
	APPLY_NOT_FOUND(404, "4003", Level.INFO, "신청 조회 실패", "신청 정보를 찾을 수 없습니다."),
	APPLY_NOT_OPENED(409, "4004", Level.INFO, "신청 오픈 전 신청", "아직 신청 기간이 아닙니다."),
	APPLY_CLOSED(409, "4005", Level.INFO, "신청 마감 후 신청", "신청이 마감되었습니다."),
	APPLY_FULL(409, "4006", Level.INFO, "정원과 대기 정원이 모두 참", "정원이 모두 찼습니다."),
	APPLY_MEMBERS_ONLY(403, "4007", Level.INFO, "멤버 전용 클럽에 미가입 회원 신청", "클럽 멤버만 신청할 수 있습니다."),
	APPLY_BLOCKED_BY_PENALTY(403, "4008", Level.INFO, "노쇼 패널티 중 신청", "노쇼 패널티 기간에는 신청할 수 없습니다."),
	APPLY_NOT_OWNER(403, "4009", Level.WARN, "다른 회원의 신청 변경 시도", "본인의 신청만 변경할 수 있습니다."),

	// 5xxx 출석
	ATTENDANCE_NOT_OPENED(409, "5001", Level.INFO, "출석 체크를 열지 않은 회차", "아직 출석 체크가 열리지 않았습니다."),
	ATTENDANCE_WINDOW_CLOSED(409, "5002", Level.INFO, "출석 인정 시간 이후 체크", "출석 인정 시간이 지났습니다."),
	ATTENDANCE_ALREADY_RECORDED(409, "5003", Level.INFO, "출석 기록 중복", "이미 출석이 기록되었습니다."),
	ATTENDANCE_APPLY_NOT_CONFIRMED(409, "5004", Level.INFO, "확정되지 않은 신청의 출석 체크", "확정된 신청만 출석할 수 있습니다."),
	ATTENDANCE_NO_SHOW_FINALIZED(409, "5005", Level.INFO, "확정된 노쇼 변경 시도", "노쇼로 확정된 기록은 바꿀 수 없습니다."),
	ATTENDANCE_INVALID_TOKEN(400, "5006", Level.WARN, "유효하지 않거나 만료된 출석 토큰", "출석 코드가 만료되었습니다. 다시 확인해 주세요."),
	ATTENDANCE_OUT_OF_RANGE(400, "5007", Level.INFO, "허용 반경 밖에서 셀프 체크", "모임 장소 근처에서 체크해 주세요."),

	// 6xxx 노쇼 패널티
	PENALTY_NOT_FOUND(404, "6001", Level.INFO, "패널티 조회 실패", "패널티 정보를 찾을 수 없습니다."),
	PENALTY_ALREADY_RELEASED(409, "6002", Level.INFO, "이미 해제된 패널티", "이미 해제된 패널티입니다."),

	// 7xxx 클럽장 승계·투표
	DELEGATION_TARGET_INVALID(409, "7001", Level.INFO, "활동 중인 멤버가 아닌 위임 대상", "활동 중인 멤버에게만 위임할 수 있습니다."),
	ABSENCE_REPORT_ALREADY_EXISTS(409, "7002", Level.INFO, "진행 중인 부재 신고 중복", "이미 진행 중인 부재 신고가 있습니다."),
	VOTE_NOT_FOUND(404, "7003", Level.INFO, "투표 조회 실패", "투표를 찾을 수 없습니다."),
	VOTE_ALREADY_IN_PROGRESS(409, "7004", Level.INFO, "진행 중인 투표가 있음", "이미 진행 중인 투표가 있습니다."),
	VOTE_COOLDOWN(409, "7005", Level.INFO, "부결 후 7일 이내 투표 개시", "부결된 지 7일이 지나야 다시 투표를 열 수 있습니다."),
	VOTE_NOT_ELIGIBLE(403, "7006", Level.INFO, "투표 또는 출마 자격 없음", "이 투표에 참여할 수 없습니다."),
	VOTE_ALREADY_CAST(409, "7007", Level.INFO, "중복 투표", "이미 투표했습니다."),
	VOTE_CLOSED(409, "7008", Level.INFO, "종료된 투표에 투표", "종료된 투표입니다."),

	// 8xxx 인증
	UNAUTHORIZED(401, "8001", Level.INFO, "로그인 필요", "로그인이 필요합니다."),
	INVALID_TOKEN(401, "8002", Level.WARN, "유효하지 않은 인증 토큰", "다시 로그인해 주세요."),
	EXPIRED_TOKEN(401, "8003", Level.INFO, "만료된 인증 토큰", "로그인이 만료되었습니다. 다시 로그인해 주세요."),
	KAKAO_LOGIN_FAILED(502, "8004", Level.ERROR, "카카오 로그인 연동 실패", "카카오 로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.");

	private final int status;
	private final String code;
	private final Level logLevel;
	private final String internalMessage; // 로그용
	private final String externalMessage; // 응답용
}
