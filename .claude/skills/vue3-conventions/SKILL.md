---
name: vue3-conventions
description: Vue 3 + TypeScript conventions for this project: Composition API, single-file components, composables, Pinia stores, routing, typed API client. Use when writing or reviewing any Vue component, store, route or frontend API code.
---

# Vue 3 conventions

## Components
- Always `<script setup lang="ts">`. No Options API.
- Props and emits are typed with `defineProps<{...}>()` and `defineEmits<{...}>()`.
- Keep a component to one job. Extract logic into a composable when it reaches about 40 lines of script.
- Template conditionals and loops use real keys: `v-for` with `:key` on a stable id.

## Structure
- `src/features/<feature>/`: `components/`, `composables/`, `stores/`, `api.ts`, `types.ts`.
- `src/components/`: only feature-agnostic UI.
- Components never call `fetch`. They call functions in the feature's `api.ts`, which use the shared client in `src/api/`.

## State
- Local state: `ref` and `computed`.
- Shared state: a Pinia setup store (`defineStore('auction', () => {...})`).
- Do not use `reactive()` for primitives. Do not destructure reactive objects without `toRefs`.

## Routing
- Lazy-load route components: `component: () => import('@/features/auction/views/AuctionList.vue')`.
- Route params are validated before use.

## Types
- No `any`. Use `unknown` and narrow.
- API types mirror the backend response records and live in the feature's `types.ts`.
