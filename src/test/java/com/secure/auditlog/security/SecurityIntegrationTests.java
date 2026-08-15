package com.secure.auditlog.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

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
	void protectsAuditEndpointsAndAllowsAnAuthorizedReader() throws Exception {
		mockMvc.perform(get("/audit/verify"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/audit/verify").with(httpBasic("audit-service", "test-only-password")))
				.andExpect(status().isOk());
	}
}
