package com.secure.auditlog.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTests {

	@Test
	void rejectsRequestsBeyondTheConfiguredWindow() throws Exception {
		Clock clock = Clock.fixed(Instant.parse("2026-08-15T12:00:30Z"), ZoneOffset.UTC);
		RateLimitFilter filter = new RateLimitFilter(new RateLimitProperties(2, 10), clock);

		assertEquals(200, invoke(filter).getStatus());
		assertEquals(200, invoke(filter).getStatus());
		MockHttpServletResponse rejected = invoke(filter);
		assertEquals(429, rejected.getStatus());
		assertEquals("30", rejected.getHeader("Retry-After"));
		assertEquals("0", rejected.getHeader("X-RateLimit-Remaining"));
	}

	@Test
	void healthChecksBypassRateLimiting() throws Exception {
		RateLimitFilter filter = new RateLimitFilter(new RateLimitProperties(1, 10),
				Clock.fixed(Instant.parse("2026-08-15T12:00:30Z"), ZoneOffset.UTC));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		assertEquals(200, response.getStatus());
	}

	private MockHttpServletResponse invoke(RateLimitFilter filter) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/audit/events");
		request.setRemoteAddr("192.0.2.10");
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		return response;
	}
}
