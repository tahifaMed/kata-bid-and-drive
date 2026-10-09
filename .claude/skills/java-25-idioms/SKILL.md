---
name: java-25-idioms
description: Idiomatic modern Java (Java 25) for this project: records, sealed types, pattern matching, switch expressions, virtual threads, immutability. Use when writing or refactoring Java code.
---

# Java 25 idioms

- Use `record` for immutable data and DTOs. Use a class with behaviour for domain entities.
- Model closed sets with `sealed` interfaces and records, then switch on them with pattern matching and no default branch:
  ```java
  sealed interface BidResult permits Accepted, Rejected {}
  record Accepted(Bid bid) implements BidResult {}
  record Rejected(String reason) implements BidResult {}

  String message = switch (result) {
      case Accepted a -> "bid " + a.bid().amount() + " accepted";
      case Rejected r -> "rejected: " + r.reason();
  };
  ```
- Prefer `var` only when the type is obvious from the right-hand side.
- Use `Optional` as a return type, never as a field or parameter.
- Use `List.of`, `Map.of`, `Set.copyOf` for immutable collections. Use `Stream` only when it reads better than a loop.
- Use virtual threads (enabled through Spring Boot configuration) rather than hand-managed thread pools.
- Use `Instant`, `Duration` and `BigDecimal`. Never use `double` for money or `Date`.
- Do not add Lombok, Guava or other libraries when the JDK already covers the need.
