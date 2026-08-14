package com.secure.auditlog.shared.error;

import com.secure.auditlog.audit.application.InvalidAuditEventException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(InvalidAuditEventException.class)
	ProblemDetail handleInvalidAuditEvent(InvalidAuditEventException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
		problem.setTitle("Invalid audit event request");
		return problem;
	}
}
