package com.secure.auditlog.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService(AuditSecurityProperties properties, PasswordEncoder passwordEncoder) {
		return new InMemoryUserDetailsManager(User.withUsername(properties.username())
				.password(passwordEncoder.encode(properties.password()))
				.roles(properties.roles().toArray(String[]::new))
				.build());
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, AuditSecurityProperties properties) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(Customizer.withDefaults())
				.authorizeHttpRequests(authorize -> {
					authorize.requestMatchers("/actuator/health/**", "/actuator/info").permitAll();
					if (properties.swaggerPublic()) {
						authorize.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll();
					} else {
						authorize.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").hasRole("AUDIT_READER");
					}
					authorize.requestMatchers("/h2-console/**").hasRole("SYSTEM_ADMIN")
							.requestMatchers(HttpMethod.POST, "/audit/events/*/redactions").hasRole("AUDIT_PRIVACY_OFFICER")
							.requestMatchers("/compliance/**").hasRole("COMPLIANCE_OFFICER")
							.requestMatchers(HttpMethod.POST, "/audit/events").hasRole("AUDIT_WRITER")
							.requestMatchers("/audit/**").hasRole("AUDIT_READER")
							.anyRequest().authenticated();
				})
				.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
				.build();
	}
}
