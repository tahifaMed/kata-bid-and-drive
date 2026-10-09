---
name: spring-boot-conventions
description: Spring Boot 4 backend conventions for this project: layering, dependency injection, DTOs, error handling, configuration. Use when writing or reviewing any backend controller, service, configuration or REST code.
---

# Spring Boot conventions

## Layering
- `domain`: plain Java. Entities and value objects with behaviour, no framework annotations. Invariants live here, not in controllers.
- `application`: use cases. Depends on domain and on port interfaces.
- `adapter.in.web`: controllers. Parse input, call a use case, map to a response. No business rules.
- `adapter.out.persistence`: JPA entities, Spring Data repositories, mappers between JPA entities and domain objects.

## Rules
- Constructor injection only. Lombok is not used. Make injected fields `private final`.
- Controllers return response records, never JPA entities or domain objects.
- Request DTOs are records with Bean Validation annotations. Validate at the edge with `@Valid`.
- Errors: `@RestControllerAdvice` returning `ProblemDetail` (RFC 9457). Map domain exceptions to 400, 404, 409 explicitly.
- Configuration: `@ConfigurationProperties` records, not scattered `@Value`.
- Time: inject `java.time.Clock`. Money: `BigDecimal`, scale 2, `RoundingMode.HALF_EVEN`.
- Transactions: `@Transactional` on use cases, not on controllers.
- Logging: SLF4J with placeholders. Never log request bodies that contain user data.

## Example: request and response
```java
public record CreateAuctionRequest(
        @NotBlank String carModel,
        @NotNull @DecimalMin("0.01") BigDecimal startingPrice) {}

public record AuctionResponse(String id, String carModel, BigDecimal startingPrice, String status) {}
```
