package com.secure.auditlog.redaction;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record RedactAuditEventRequest(@NotEmpty @Size(max = 50) List<String> paths) {
}
