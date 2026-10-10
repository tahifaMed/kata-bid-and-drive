# 0007: GET /api/users returns a plain array

Status: Accepted

## Context
The roadmap says responses are HAL with `_links`, and CLAUDE.md says the API is hypermedia-style. The users list is four fixed seed rows with no state transitions. The only thing a client can do with a user is pick it as the acting user.

## Decision
- `GET /api/users` returns a bare JSON array of `{id, displayName}`, with no `_links` and no HAL envelope.
- Other collections and resources keep the HAL convention from the roadmap.

## Consequences
- The switcher needs no link following, so the plain shape costs nothing today.
- If users ever get links (for example a profile), this decision should be revisited, and the array can move to a HAL collection under a new path or version.
