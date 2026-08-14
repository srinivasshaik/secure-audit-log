package com.secure.auditlog.audit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class CanonicalJsonServiceTests {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final CanonicalJsonService canonicalJsonService = new CanonicalJsonService(objectMapper);

	@Test
	void sortsObjectPropertiesRecursivelyButPreservesArrayOrder() throws Exception {
		String canonical = canonicalJsonService.canonicalizeObject(
				objectMapper.readTree("{\"z\":1,\"nested\":{\"b\":2,\"a\":3},\"items\":[{\"d\":4,\"c\":5},2]}"));

		assertEquals("{\"items\":[{\"c\":5,\"d\":4},2],\"nested\":{\"a\":3,\"b\":2},\"z\":1}", canonical);
	}
}
