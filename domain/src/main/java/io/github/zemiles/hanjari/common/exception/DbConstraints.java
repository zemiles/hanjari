package io.github.zemiles.hanjari.common.exception;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * DB 제약 이름과 ErrorType을 이어 주는 표.
 * service의 사전 확인을 통과한 동시 요청이 DB 제약에 걸리면 GlobalExceptionHandler가 이 표로 응답을 정한다
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DbConstraints {

	// 사용자 요청 때문에 걸릴 수 있는 유니크 제약만 넣는다.
	// 멱등키·대기 번호처럼 service가 직접 처리해야 하는 제약은 넣지 않는다 (HJR-93 참고)
	private static final Map<String, ErrorType> ERROR_TYPES = Map.of(
		"uk_member_kakao_id", ErrorType.MEMBER_ALREADY_REGISTERED,
		"uk_member_nickname_active", ErrorType.MEMBER_NICKNAME_DUPLICATED,
		"uk_club_membership_member_club", ErrorType.CLUB_ALREADY_JOINED,
		"uk_club_membership_vice_owner", ErrorType.CLUB_VICE_OWNER_ALREADY_EXISTS,
		"uk_apply_active", ErrorType.APPLY_ALREADY_EXISTS,
		"uk_attendance_apply", ErrorType.ATTENDANCE_ALREADY_RECORDED
	);

	/** 걸린 제약 이름. 알 수 없으면 null */
	public static String violatedName(DataIntegrityViolationException exception) {
		return exception.getCause() instanceof ConstraintViolationException violation
			? violation.getConstraintName() : null;
	}

	/** 표에 있는 제약이면 그 ErrorType */
	public static Optional<ErrorType> errorTypeOf(DataIntegrityViolationException exception) {
		String name = violatedName(exception);
		return name == null ? Optional.empty() : Optional.ofNullable(ERROR_TYPES.get(name));
	}

	/** 마이그레이션으로 생성된 실제 유니크 인덱스와 표를 대조할 때 쓴다 */
	static Set<String> mappedNames() {
		return ERROR_TYPES.keySet();
	}

}
