package com.secure.auditlog.audit.application;

public class InvalidAuditEventException extends RuntimeException {

	public InvalidAuditEventException(String message) {
		super(message);
	}
}
