package com.secure.auditlog.audit.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class AuditHashingService {

	private static final String HASH_ALGORITHM = "SHA-256";
	private static final String CANONICALIZATION_VERSION = "audit-log-content-v1";

	public String contentHash(AuditEventContent content) {
		try {
			MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
			writeField(digest, CANONICALIZATION_VERSION);
			writeField(digest, content.eventType());
			writeField(digest, content.actorId());
			writeField(digest, content.resourceType());
			writeField(digest, content.resourceId());
			writeField(digest, content.canonicalPayload());
			writeField(digest, canonicalTimestamp(content.occurredAt()));
			return HexFormat.of().formatHex(digest.digest());
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
		}
	}

	private void writeField(MessageDigest digest, String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
		digest.update((byte) ':');
		digest.update(bytes);
	}

	private String canonicalTimestamp(Instant timestamp) {
		return timestamp.toString();
	}
}
