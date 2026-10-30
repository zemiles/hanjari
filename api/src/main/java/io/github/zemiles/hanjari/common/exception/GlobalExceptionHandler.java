package io.github.zemiles.hanjari.common.exception;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	// 우리가 던진 예외: ErrorType에 다 들어 있다
	@ExceptionHandler(HanjariException.class)
	public ResponseEntity<ErrorResponse> handleHanjariException(HanjariException exception) {
		ExceptionLogger.log(exception);
		return toResponse(exception.getErrorType());
	}

	// @Valid 검증 실패: 어떤 필드가 틀렸는지만 로그에 남긴다
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
		List<String> fields = exception.getBindingResult().getFieldErrors().stream()
			.map(FieldError::getField)
			.toList();
		logAt(ErrorType.VALID_ERROR, "fields=" + fields);
		return toResponse(ErrorType.VALID_ERROR);
	}

	// JSON 형식 오류, 없는 주소, 지원 안 하는 메서드: 원래 4xx인 스프링 예외들
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException exception) {
		logAt(ErrorType.INVALID_REQUEST_BODY, exception.getMessage());
		return toResponse(ErrorType.INVALID_REQUEST_BODY);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException exception) {
		logAt(ErrorType.RESOURCE_NOT_FOUND, exception.getResourcePath());
		return toResponse(ErrorType.RESOURCE_NOT_FOUND);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
		logAt(ErrorType.METHOD_NOT_ALLOWED, exception.getMethod());
		return toResponse(ErrorType.METHOD_NOT_ALLOWED);
	}

	// 나머지 전부: 스택 트레이스는 로그에만, 응답에는 일반 메시지만
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
		log.error("[{}] 처리하지 못한 예외", ErrorType.SYSTEM_ERROR.name(), exception);
		return toResponse(ErrorType.SYSTEM_ERROR);
	}

	private ResponseEntity<ErrorResponse> toResponse(ErrorType errorType) {
		return ResponseEntity.status(errorType.getStatus()).body(ErrorResponse.of(errorType));
	}

	// ErrorType에 정한 로그 레벨로 남긴다
	private void logAt(ErrorType errorType, String detail) {
		log.atLevel(errorType.getLogLevel()).log("[{}] {}", errorType.name(), detail);
	}

	// DB 제약 위반: 표에 있으면 그 에러로, 없으면 DB_ERROR(코드 실수라 눈에 띄게)
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
		String constraint = DbConstraints.violatedName(exception);
		Optional<ErrorType> errorType = DbConstraints.errorTypeOf(exception);

		if (errorType.isPresent()) {
			logAt(errorType.get(), "constraint=" + constraint);
			return toResponse(errorType.get());
		}
		log.error("[{}] 처리하지 못한 DB 제약 위반 constraint={}", ErrorType.DB_ERROR.name(), constraint, exception);
		return toResponse(ErrorType.DB_ERROR);
	}
}
