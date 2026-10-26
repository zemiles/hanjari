package io.github.zemiles.hanjari.common.exception;

import org.slf4j.spi.LoggingEventBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExceptionLogger {

	private ExceptionLogger() {
	}

	public static void log(HanjariException e) {
		LoggingEventBuilder event = log.atLevel(e.getErrorType().getLogLevel())
			.setMessage("[{}] {}")
			.addArgument(e.getErrorType().name())
			.addArgument(e.getMessage())
			.setCause(e.getCause());

		e.getParameter().forEach(event::addKeyValue);
		event.log();
	}
}
