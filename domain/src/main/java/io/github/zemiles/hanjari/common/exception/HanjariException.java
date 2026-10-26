package io.github.zemiles.hanjari.common.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;

@Getter
public class HanjariException extends RuntimeException {

	private final ErrorType errorType;
	private final Map<String, Object> parameter;

	public HanjariException(ErrorType errorType, Map<String, Object> parameter) {
		this(errorType, parameter, null);
	}

	public HanjariException(ErrorType errorType, Map<String, Object> parameter, Throwable cause) {
		super(errorType.getInternalMessage(), cause);
		this.errorType = errorType;
		this.parameter = parameter == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(parameter));
	}
}
