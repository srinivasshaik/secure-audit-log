package com.secure.auditlog.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void exposesHealthWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(header().exists("X-Correlation-ID"));
	}

	@Test
	void protectsSwaggerDocumentationInDev() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectsH2ConsoleInDev() throws Exception {
		mockMvc.perform(get("/h2-console/"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().exists("WWW-Authenticate"));
	}

	@Test
	void allowsAuthorizedWriterToCreateAnAuditEvent() throws Exception {
		mockMvc.perform(post("/audit/events")
				.with(httpBasic("audit-service", "test-only-password"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"eventType":"USER_LOGIN","actorId":"api-actor","resourceType":"ACCOUNT",
						 "resourceId":"api-account","payload":{"channel":"web"}}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/audit/events/")))
				.andExpect(jsonPath("$.eventType").value("USER_LOGIN"))
				.andExpect(jsonPath("$.contentHash").isString());
	}

	@Test
	void rejectsInvalidCreateRequestAtTheHttpBoundary() throws Exception {
		mockMvc.perform(post("/audit/events")
				.with(httpBasic("audit-service", "test-only-password"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"eventType\":\"\",\"payload\":{}}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void translatesQueryValidationFailuresToProblemDetails() throws Exception {
		mockMvc.perform(get("/audit/events").param("size", "0")
				.with(httpBasic("audit-service", "test-only-password")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid audit event request"))
				.andExpect(jsonPath("$.detail").value("size must be between 1 and 200"));
	}

	@Test
	void protectsAuditEndpointsAndAllowsAnAuthorizedReader() throws Exception {
		mockMvc.perform(get("/audit/verify"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/audit/verify").with(httpBasic("audit-service", "test-only-password")))
				.andExpect(status().isOk());
	}
}
