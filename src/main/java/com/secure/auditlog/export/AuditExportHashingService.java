package com.secure.auditlog.export;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class AuditExportHashingService {

	public String hash(String formatVersion, String exportedAt, String selectorType, String selectorValue, long chainHeadSequence,
			String chainHeadHash, java.util.List<AuditExportEvent> events) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			write(digest, formatVersion);
			write(digest, exportedAt);
			write(digest, selectorType);
			write(digest, selectorValue);
			write(digest, Long.toString(chainHeadSequence));
			write(digest, chainHeadHash);
			for (AuditExportEvent event : events) {
				write(digest, event.id().toString());
				write(digest, Long.toString(event.chainSequence()));
				write(digest, event.eventType());
				write(digest, event.actorId());
				write(digest, event.resourceType());
				write(digest, event.resourceId());
				write(digest, event.payload().toString());
				write(digest, event.occurredAt().toString());
				write(digest, event.previousHash());
				write(digest, event.contentHash());
				AuditExportRedaction redaction = event.redaction();
				write(digest, redaction == null ? null : redaction.redactedPaths());
				write(digest, redaction == null ? null : redaction.redactedAt().toString());
				write(digest, redaction == null ? null : redaction.originalContentHash());
				write(digest, redaction == null ? null : redaction.redactionHash());
			}
			return HexFormat.of().formatHex(digest.digest());
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
		}
	}

	private void write(MessageDigest digest, String value) {
		if (value == null) {
			digest.update((byte) 0);
			return;
		}
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		digest.update((byte) 1);
		digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
		digest.update((byte) ':');
		digest.update(bytes);
	}
}
