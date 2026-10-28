package io.github.zemiles.hanjari.common.exception;

public record ErrorResponse(int status, String code, String message) {
	public static ErrorResponse of(ErrorType errorType) {
		return new ErrorResponse(errorType.getStatus(), errorType.getCode(), errorType.getExternalMessage());
	}
}
