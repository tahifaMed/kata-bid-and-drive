<script setup lang="ts">
import { useCreateAuctionForm } from '../composables/useCreateAuctionForm'
import type { AuctionFormField } from '../auctionForm'
import type { AuctionResponse } from '../types'

const emit = defineEmits<{ created: [auction: AuctionResponse] }>()

const form = useCreateAuctionForm()
const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone

const carFields: { name: AuctionFormField; label: string; type: string; hint?: string }[] = [
  { name: 'make', label: 'Make', type: 'text' },
  { name: 'model', label: 'Model', type: 'text' },
  { name: 'year', label: 'Year', type: 'number' },
  { name: 'mileageKm', label: 'Mileage (km)', type: 'number' },
  { name: 'imageUrl', label: 'Photo link (optional)', type: 'url', hint: 'Starts with https://' },
]

const auctionFields: { name: AuctionFormField; label: string; type: string; hint?: string }[] = [
  { name: 'startingPrice', label: 'Starting price', type: 'text', hint: 'For example 12000.00' },
  { name: 'minIncrement', label: 'Minimum bid increment', type: 'text', hint: 'For example 100.00' },
  {
    name: 'endsAt',
    label: 'Auction ends',
    type: 'datetime-local',
    hint: `Your time zone: ${timeZone}. Between 1 hour and 30 days from now.`,
  },
]

function errorId(name: AuctionFormField): string {
  return `${name}-error`
}

function describedBy(name: AuctionFormField, hasHint: boolean): string | undefined {
  const ids = [hasHint ? hintId(name) : null, form.visibleError(name) ? errorId(name) : null].filter(Boolean)
  return ids.length > 0 ? ids.join(' ') : undefined
}

function hintId(name: AuctionFormField): string {
  return `${name}-hint`
}

async function onSubmit(): Promise<void> {
  const auction = await form.submit()
  if (auction) emit('created', auction)
}
</script>

<template>
  <form class="stack-section" novalidate @submit.prevent="onSubmit">
    <fieldset class="flex flex-col gap-6 min-w-0">
      <legend class="text-display font-bold">The car</legend>

      <div v-for="field in carFields" :key="field.name" class="flex flex-col gap-2">
        <label :for="field.name" class="font-bold">{{ field.label }}</label>
        <input
          :id="field.name"
          v-model="form.values[field.name]"
          :type="field.type"
          class="field"
          :aria-invalid="form.visibleError(field.name) ? 'true' : undefined"
          :aria-describedby="describedBy(field.name, Boolean(field.hint))"
          @blur="form.touch(field.name)"
        />
        <p v-if="field.hint" :id="hintId(field.name)" class="text-meta text-ink-muted">{{ field.hint }}</p>
        <p v-if="form.visibleError(field.name)" :id="errorId(field.name)" class="text-meta text-danger" role="alert">
          {{ form.visibleError(field.name) }}
        </p>
      </div>

      <div class="flex flex-col gap-2">
        <label for="description" class="font-bold">Description (optional)</label>
        <textarea
          id="description"
          v-model="form.values.description"
          class="field"
          rows="4"
          :aria-invalid="form.visibleError('description') ? 'true' : undefined"
          :aria-describedby="describedBy('description', false)"
          @blur="form.touch('description')"
        />
        <p v-if="form.visibleError('description')" :id="errorId('description')" class="text-meta text-danger" role="alert">
          {{ form.visibleError('description') }}
        </p>
      </div>
    </fieldset>

    <fieldset class="flex flex-col gap-6 min-w-0">
      <legend class="text-display font-bold">The auction</legend>

      <div v-for="field in auctionFields" :key="field.name" class="flex flex-col gap-2">
        <label :for="field.name" class="font-bold">{{ field.label }}</label>
        <input
          :id="field.name"
          v-model="form.values[field.name]"
          :type="field.type"
          class="field"
          :aria-invalid="form.visibleError(field.name) ? 'true' : undefined"
          :aria-describedby="describedBy(field.name, true)"
          @blur="form.touch(field.name)"
        />
        <p :id="hintId(field.name)" class="text-meta text-ink-muted">{{ field.hint }}</p>
        <p v-if="form.visibleError(field.name)" :id="errorId(field.name)" class="text-meta text-danger" role="alert">
          {{ form.visibleError(field.name) }}
        </p>
      </div>
    </fieldset>

    <p v-if="form.failed.value" class="text-danger" role="alert">
      Could not create the auction. Try again.
    </p>

    <button type="submit" class="btn btn-primary" :disabled="!form.isValid.value || form.submitting.value">
      {{ form.submitting.value ? 'Creating auction…' : 'Create auction' }}
    </button>
  </form>
</template>
