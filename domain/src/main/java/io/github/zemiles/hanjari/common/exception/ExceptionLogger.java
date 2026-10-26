package io.github.zemiles.hanjari.common.exception;

import org.slf4j.spi.LoggingEventBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExceptionLogger {

	private ExceptionLogger() {
	}

	public static void log(HanjariException exception) {
		LoggingEventBuilder event = log.atLevel(exception.getErrorType().getLogLevel())
			.setMessage("[{}] {}")
			.addArgument(exception.getErrorType().name())
			.addArgument(exception.getMessage())
			.setCause(exception.getCause());

		exception.getParameter().forEach(event::addKeyValue);
		event.log();
	}
}
