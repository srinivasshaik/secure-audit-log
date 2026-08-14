package com.secure.auditlog.audit.domain;

import java.util.Comparator;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Component
public class CanonicalJsonService {

	private final ObjectMapper objectMapper;

	public CanonicalJsonService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public String canonicalizeObject(JsonNode payload) {
		if (payload == null || !payload.isObject()) {
			throw new IllegalArgumentException("payload must be a JSON object");
		}
		try {
			return objectMapper.writeValueAsString(canonicalize(payload));
		} catch (JacksonException exception) {
			throw new IllegalArgumentException("payload could not be canonicalized", exception);
		}
	}

	public JsonNode read(String payload) {
		try {
			return objectMapper.readTree(payload);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Stored audit payload is not valid JSON", exception);
		}
	}

	public String canonicalizeStoredPayload(String payload) {
		return canonicalizeObject(read(payload));
	}

	private JsonNode canonicalize(JsonNode node) {
		if (node.isObject()) {
			ObjectNode result = objectMapper.createObjectNode();
			node.properties().stream()
					.sorted(Comparator.comparing(entry -> entry.getKey()))
					.forEach(entry -> result.set(entry.getKey(), canonicalize(entry.getValue())));
			return result;
		}
		if (node.isArray()) {
			ArrayNode result = objectMapper.createArrayNode();
			for (JsonNode element : node) {
				result.add(canonicalize(element));
			}
			return result;
		}
		return node;
	}
}
