---
name: spring-testing
description: Testing strategy and conventions for the Spring Boot backend: unit tests, slice tests, integration tests, concurrency tests. Use when writing or reviewing backend tests.
---

# Spring testing

## Pick the smallest test that proves the behaviour
- Domain rules: plain JUnit 5 + AssertJ. No Spring context. This is most of the test suite.
- Use cases: JUnit 5 with hand-written fakes for ports, or `@MockitoBean` when a fake is overkill.
- Web layer: `@WebMvcTest` with `@MockitoBean` for the use cases. Check status codes, JSON and `ProblemDetail` bodies.
- Persistence: `@DataJpaTest` against PostgreSQL via Testcontainers. Check mappings and queries.
- End to end: `@SpringBootTest(webEnvironment = RANDOM_PORT)` only for a few critical flows.

## Rules
- Use `@MockitoBean`. `@MockBean` is deprecated.
- Name tests by behaviour: `rejects_bid_not_higher_than_current_highest()`.
- One behaviour per test. Arrange, act, assert, in that order.
- Inject a fixed `Clock` in tests. Never depend on the wall clock.
- Concurrency: for the bid race, run N threads with a `CountDownLatch` against the real persistence layer. Assert that exactly one winner is recorded when the rules say so.
- Never mark a test `@Disabled` to get a build green.
