package com.secure.auditlog.compliance;

import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/compliance/client-account-access")
public class ComplianceReportingController {

	private final ComplianceReportingService reportingService;

	public ComplianceReportingController(ComplianceReportingService reportingService) {
		this.reportingService = reportingService;
	}

	@GetMapping
	public ClientAccountAccessReport report(
			@RequestParam(required = false) String actorId,
			@RequestParam(required = false) String resourceId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return reportingService.report(actorId, resourceId, from, to, page, size);
	}
}
