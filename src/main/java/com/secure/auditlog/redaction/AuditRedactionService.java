package com.secure.auditlog.redaction;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.secure.auditlog.audit.application.InvalidAuditEventException;
import com.secure.auditlog.audit.domain.CanonicalJsonService;
import com.secure.auditlog.audit.infrastructure.AuditEventEntity;
import com.secure.auditlog.audit.infrastructure.AuditEventJpaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Service
public class AuditRedactionService {

	private static final String REDACTED_VALUE = "[REDACTED]";

	private final AuditEventJpaRepository auditEventRepository;
	private final AuditEventRedactionRepository redactionRepository;
	private final CanonicalJsonService canonicalJsonService;
	private final AuditRedactionHashingService redactionHashingService;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public AuditRedactionService(AuditEventJpaRepository auditEventRepository,
			AuditEventRedactionRepository redactionRepository, CanonicalJsonService canonicalJsonService,
			AuditRedactionHashingService redactionHashingService, ObjectMapper objectMapper, Clock clock) {
		this.auditEventRepository = auditEventRepository;
		this.redactionRepository = redactionRepository;
		this.canonicalJsonService = canonicalJsonService;
		this.redactionHashingService = redactionHashingService;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Transactional
	public AuditEventRedactionEntity redact(UUID eventId, List<String> paths) {
		validatePaths(paths);
		if (redactionRepository.existsById(eventId)) {
			throw new InvalidAuditEventException("event has already been redacted");
		}
		AuditEventEntity event = auditEventRepository.findById(eventId)
				.orElseThrow(() -> new InvalidAuditEventException("audit event was not found"));
		JsonNode redactedPayload = canonicalJsonService.read(event.getPayload()).deepCopy();
		for (String path : paths) {
			redactPath(redactedPayload, path);
		}
		String canonicalPayload = canonicalJsonService.canonicalizeObject(redactedPayload);
		String canonicalPaths = canonicalJsonService.canonicalizeStringArray(paths);
		// Keep certificate input stable after database timestamp serialization.
		Instant redactedAt = clock.instant().truncatedTo(ChronoUnit.MICROS);
		String redactionHash = redactionHashingService.redactionHash(event.getContentHash(), canonicalPayload, canonicalPaths,
				redactedAt);

		event.replacePayload(canonicalPayload);
		return redactionRepository.save(new AuditEventRedactionEntity(eventId, canonicalPayload, canonicalPaths, redactedAt,
				event.getContentHash(), redactionHash));
	}

	private void validatePaths(List<String> paths) {
		if (paths == null || paths.isEmpty()) {
			throw new InvalidAuditEventException("at least one JSON Pointer path is required");
		}
		if (paths.size() > 50 || new HashSet<>(paths).size() != paths.size()) {
			throw new InvalidAuditEventException("paths must contain between 1 and 50 unique values");
		}
		if (paths.stream().anyMatch(path -> path == null || !path.startsWith("/") || path.equals("/"))) {
			throw new InvalidAuditEventException("each path must be a non-root JSON Pointer");
		}
	}

	private void redactPath(JsonNode root, String path) {
		String[] tokens = path.substring(1).split("/", -1);
		JsonNode parent = root;
		for (int index = 0; index < tokens.length - 1; index++) {
			parent = child(parent, decodeToken(tokens[index]), path);
		}
		String finalToken = decodeToken(tokens[tokens.length - 1]);
		if (parent instanceof ObjectNode objectNode) {
			JsonNode target = objectNode.get(finalToken);
			validateLeaf(target, path);
			objectNode.set(finalToken, objectMapper.stringNode(REDACTED_VALUE));
			return;
		}
		if (parent instanceof ArrayNode arrayNode) {
			int index = arrayIndex(finalToken, path);
			JsonNode target = arrayNode.get(index);
			validateLeaf(target, path);
			arrayNode.set(index, objectMapper.stringNode(REDACTED_VALUE));
			return;
		}
		throw new InvalidAuditEventException("JSON Pointer does not address a container: " + path);
	}

	private JsonNode child(JsonNode parent, String token, String path) {
		if (parent instanceof ObjectNode objectNode) {
			JsonNode child = objectNode.get(token);
			if (child != null) return child;
		} else if (parent instanceof ArrayNode arrayNode) {
			return arrayNode.get(arrayIndex(token, path));
		}
		throw new InvalidAuditEventException("JSON Pointer was not found: " + path);
	}

	private int arrayIndex(String token, String path) {
		try {
			int index = Integer.parseInt(token);
			if (index < 0) throw new NumberFormatException();
			return index;
		} catch (NumberFormatException exception) {
			throw new InvalidAuditEventException("JSON Pointer has an invalid array index: " + path);
		}
	}

	private void validateLeaf(JsonNode target, String path) {
		if (target == null) {
			throw new InvalidAuditEventException("JSON Pointer was not found: " + path);
		}
		if (target.isArray() || target.isObject()) {
			throw new InvalidAuditEventException("JSON Pointer must target a scalar value: " + path);
		}
	}

	private String decodeToken(String token) {
		return token.replace("~1", "/").replace("~0", "~");
	}
}
