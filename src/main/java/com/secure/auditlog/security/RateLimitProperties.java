package com.secure.auditlog.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "audit.rate-limit")
public record RateLimitProperties(int requestsPerMinute, int maxClients) {
	public RateLimitProperties {
		if (requestsPerMinute < 1) throw new IllegalArgumentException("requests-per-minute must be positive");
		if (maxClients < 1) throw new IllegalArgumentException("max-clients must be positive");
	}
}
