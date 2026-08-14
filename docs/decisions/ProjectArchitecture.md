# Project Architecture

## Technology

- Java 21
- Spring Boot 3
- Maven

## Package Structure

```text
src/main/java/com/auditlog
├── AuditLogServiceApplication.java
├── controller/
├── service/
├── repository/
└── model/
```

The application follows the conventional **Controller → Service → Repository** flow:

- `controller` exposes HTTP endpoints and validates requests.
- `service` contains application and business logic.
- `repository` isolates database access.
- `model` contains audit-log entities and request/response models.

## Spring Boot Main Class

`AuditLogServiceApplication` is the application entry point. It is in the `com.auditlog` root package, so Spring Boot scans all project packages beneath it.

```java
package com.auditlog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuditLogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditLogServiceApplication.class, args);
    }
}
```

Create separate decision records here later for consequential choices such as hash canonicalization, persistence, archival, redaction, and export verification.
