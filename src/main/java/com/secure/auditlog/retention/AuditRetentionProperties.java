package com.secure.auditlog.retention;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "audit.retention")
public record AuditRetentionProperties(Duration archiveAfter, String archiveCron) {
}
