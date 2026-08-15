package com.secure.auditlog.security;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

	private final RateLimitProperties properties;
	private final Clock clock;
	private final ConcurrentHashMap<String, Window> clients = new ConcurrentHashMap<>();

	public RateLimitFilter(RateLimitProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return request.getRequestURI().startsWith("/actuator/health");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		long minute = clock.instant().truncatedTo(ChronoUnit.MINUTES).getEpochSecond();
		String client = request.getRemoteAddr();
		if (clients.size() >= properties.maxClients() && !clients.containsKey(client)) {
			clients.entrySet().removeIf(entry -> entry.getValue().minute < minute);
			if (clients.size() >= properties.maxClients()) {
				reject(response, 60);
				return;
			}
		}
		Window window = clients.compute(client, (key, current) -> current == null || current.minute != minute
				? new Window(minute) : current);
		int count = window.count.incrementAndGet();
		response.setHeader("X-RateLimit-Limit", Integer.toString(properties.requestsPerMinute()));
		response.setHeader("X-RateLimit-Remaining", Integer.toString(Math.max(0, properties.requestsPerMinute() - count)));
		if (count > properties.requestsPerMinute()) {
			long retryAfter = Math.max(1, minute + 60 - Instant.now(clock).getEpochSecond());
			reject(response, retryAfter);
			return;
		}
		chain.doFilter(request, response);
	}

	private void reject(HttpServletResponse response, long retryAfter) throws IOException {
		response.setStatus(429);
		response.setHeader("Retry-After", Long.toString(retryAfter));
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.getWriter().write("{\"title\":\"Too Many Requests\",\"status\":429}");
	}

	private static final class Window {
		private final long minute;
		private final AtomicInteger count = new AtomicInteger();
		private Window(long minute) { this.minute = minute; }
	}
}
