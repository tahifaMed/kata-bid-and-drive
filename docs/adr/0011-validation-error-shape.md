# 0011: Field-level validation errors in ProblemDetail

Status: Accepted

## Context
A form with several invalid fields needs all errors in one response, so the user fixes them together. ProblemDetail has no standard list of field errors, and the spec must name the shape for the frontend and tests.

## Decision
- Validation errors are RFC 9457 ProblemDetail with `status: 400` and an `errors` property: a list of `{field, message}`.
- `field` is the JSON path of the request field, for example `car.make` or `startingPrice`.
- All errors are returned together. The list is in request order.
- Missing identity is not a validation error. It is a 401 and is checked first.

## Consequences
- The frontend can map each error to its form field.
- Other write specs (007) can reuse the same shape.
