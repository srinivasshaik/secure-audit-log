package com.secure.auditlog.security;

import java.util.Collection;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("prod")
public class ProductionSecurityConfiguration {

	@Bean
	SecurityFilterChain productionSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.requiresChannel(channel -> channel.anyRequest().requiresSecure())
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(roleConverter())))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").hasRole("AUDIT_READER")
						.requestMatchers("/h2-console/**").denyAll()
						.requestMatchers(org.springframework.http.HttpMethod.POST, "/audit/events/*/redactions").hasRole("AUDIT_PRIVACY_OFFICER")
						.requestMatchers("/compliance/**").hasRole("COMPLIANCE_OFFICER")
						.requestMatchers(org.springframework.http.HttpMethod.POST, "/audit/events").hasRole("AUDIT_WRITER")
						.requestMatchers("/audit/**").hasRole("AUDIT_READER")
						.anyRequest().authenticated())
				.build();
	}

	private Converter<Jwt, AbstractAuthenticationToken> roleConverter() {
		return jwt -> {
			List<String> roles = jwt.getClaimAsStringList("roles");
			Collection<GrantedAuthority> authorities = roles == null ? List.of() : roles.stream()
					.map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
					.map(SimpleGrantedAuthority::new)
					.map(GrantedAuthority.class::cast)
					.toList();
			return new JwtAuthenticationToken(jwt, authorities);
		};
	}
}
