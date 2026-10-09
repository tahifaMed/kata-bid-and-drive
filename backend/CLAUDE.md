# Backend (Spring Boot 4, Java 25)

Root rules in `../CLAUDE.md` also apply.

## Commands
- Build and test everything: `./mvnw verify`
- Run the app: `./mvnw spring-boot:run` (H2 in-memory, console at `/h2-console` once enabled)
- One test class: `./mvnw -Dtest=ClassName test`

## Layout
Base package `com.carrefour.kata.bidanddrive`.
- `auction/domain`: entities, value objects, domain rules. No Spring or JPA annotations here.
- `auction/application`: use cases and ports (interfaces).
- `auction/adapter/in/web`: REST controllers and DTOs (records).
- `auction/adapter/out/persistence`: JPA entities, repositories, mappers.
- `shared`: cross-cutting pieces such as `Clock` beans and error handling.

Migrations live in `src/main/resources/db/migration`, named `V<n>__<description>.sql`.

## Rules
- Constructor injection only. No field injection.
- Controllers never return JPA entities. Use records for request and response DTOs.
- Errors use RFC 9457 `ProblemDetail`. Never leak stack traces.
- Money is `BigDecimal` with scale 2. Time is `Instant`. Inject `Clock`, never call `Instant.now()` in domain code.
- Concurrency: bids use optimistic locking (`@Version`) and a test with parallel bids.
- Tests: JUnit 5 + AssertJ. Use `@MockitoBean`, never `@MockBean`. Testcontainers only when H2 can't prove the behaviour.

## Skills to read first
- Spring Boot conventions: `spring-boot-conventions`
- Java 25 idioms: `java-25-idioms`
- Tests: `spring-testing`
- Persistence: `jpa-persistence`
