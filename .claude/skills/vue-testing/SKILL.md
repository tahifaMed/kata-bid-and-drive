---
name: vue-testing
description: Testing conventions for the Vue frontend with Vitest and Vue Test Utils: component behaviour tests, store tests, composable tests. Use when writing or reviewing frontend tests.
---

# Vue testing

- Runner: Vitest. Component tests: `@vue/test-utils` with `jsdom`. Config is in `frontend/vitest.config.ts`.
- Test what the user sees and does: text, roles, emitted events, calls to the API module. Use `getByRole` and `getByText` from `@testing-library/vue` if it is added, otherwise `wrapper.find` with semantic selectors.
- Never assert on CSS classes, component internals, or `wrapper.vm` state.
- Mock the feature's `api.ts` module with `vi.mock`. Do not mock Pinia. Use `createTestingPinia` or a real store with a fresh `setActivePinia(createPinia())` per test.
- Stores and composables: plain Vitest tests, no mounting.
- One behaviour per `it`. Name tests as sentences: `it('disables the bid button when the amount is not above the highest bid')`.
- Run a single file with `npm run test:unit -- --run path/to/file.spec.ts`.
