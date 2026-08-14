package com.secure.auditlog.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "audit.security")
public record AuditSecurityProperties(String username, String password, List<String> roles) {
}
