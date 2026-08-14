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
	void exposesSwaggerDocumentationWithoutAuthenticationInDev() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk());
	}

	@Test
	void protectsAuditEndpointsAndAllowsAnAuthorizedReader() throws Exception {
		mockMvc.perform(get("/audit/verify"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/audit/verify").with(httpBasic("audit-service", "local-dev-only-change-me")))
				.andExpect(status().isOk());
	}
}
