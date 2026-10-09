# Frontend (Vue 3, TypeScript)

Root rules in `../CLAUDE.md` also apply.

## Commands
- Dev server: `npm run dev`
- Unit tests: `npm run test:unit`
- Type check and build: `npm run build`
- Lint and format: `npm run lint`, `npm run format`

## Layout
- `src/features/auction/`: components, composables and store for the auction feature.
- `src/features/bidding/`: same shape, for bidding.
- `src/components/`: shared, feature-agnostic UI.
- `src/router/`: routes. Each feature registers its own routes.
- `src/api/`: typed HTTP client. Components never call `fetch` directly.

## Rules
- Vue 3 Composition API with `<script setup lang="ts">`. No Options API.
- Pinia stores hold shared state. Local state stays in the component.
- Follow the `vue3-conventions` skill for component structure and `vue-testing` for tests.
- Test behaviour with Vue Test Utils and Vitest. Do not test implementation details or CSS classes.
- No `any`. Model API responses with types that match the backend DTOs.

## Skills to read first
- Components and reactivity: `vue3-conventions`
- Tests: `vue-testing`
