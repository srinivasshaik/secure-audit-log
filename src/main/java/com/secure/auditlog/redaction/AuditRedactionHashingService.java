package com.secure.auditlog.redaction;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class AuditRedactionHashingService {

	public String redactionHash(String originalContentHash, String canonicalRedactedPayload, String canonicalPaths,
			Instant redactedAt) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			writeField(digest, "audit-log-redaction-v1");
			writeField(digest, originalContentHash);
			writeField(digest, canonicalRedactedPayload);
			writeField(digest, canonicalPaths);
			writeField(digest, redactedAt.toString());
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
}
