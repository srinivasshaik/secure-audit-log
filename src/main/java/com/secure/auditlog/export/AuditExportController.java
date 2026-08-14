package com.secure.auditlog.export;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit/exports")
public class AuditExportController {

	private final AuditExportService exportService;

	public AuditExportController(AuditExportService exportService) {
		this.exportService = exportService;
	}

	@GetMapping
	public AuditExportBundle export(@RequestParam(required = false) String actorId,
			@RequestParam(required = false) String resourceId) {
		return exportService.export(actorId, resourceId);
	}
}
