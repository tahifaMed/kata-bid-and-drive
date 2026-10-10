<script setup lang="ts">
import { useCreateAuctionForm } from '../composables/useCreateAuctionForm'
import type { AuctionFormField } from '../auctionForm'
import type { AuctionResponse } from '../types'

const emit = defineEmits<{ created: [auction: AuctionResponse] }>()

const form = useCreateAuctionForm()
const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone

interface FieldConfig {
  name: AuctionFormField
  label: string
  hint: string
  type: string
  wide?: boolean
}

const carFields: FieldConfig[] = [
  { name: 'make', label: 'Car make', hint: 'For example Volkswagen', type: 'text' },
  { name: 'model', label: 'Car model', hint: 'For example Golf', type: 'text' },
  { name: 'year', label: 'Year of manufacture', hint: 'From 1886 to next year', type: 'number' },
  { name: 'mileageKm', label: 'Mileage (km)', hint: 'Kilometres driven so far', type: 'number' },
  { name: 'imageUrl', label: 'Photo link (optional)', hint: 'Must start with https://', type: 'url', wide: true },
  { name: 'description', label: 'Description (optional)', hint: 'Condition, history, extras', type: 'textarea', wide: true },
]

const auctionFields: FieldConfig[] = [
  { name: 'startingPrice', label: 'Starting bid', hint: 'The first bid must be at least this', type: 'text' },
  { name: 'minIncrement', label: 'Minimum raise', hint: 'Each new bid must beat the last by this much', type: 'text' },
  {
    name: 'endsAt',
    label: 'Auction closes',
    hint: `${timeZone}. Between 1 hour and 30 days from now.`,
    type: 'datetime-local',
    wide: true,
  },
]

function errorId(name: AuctionFormField): string {
  return `${name}-error`
}

function hintId(name: AuctionFormField): string {
  return `${name}-hint`
}

function describedBy(name: AuctionFormField): string {
  const ids = [hintId(name), form.visibleError(name) ? errorId(name) : null].filter(Boolean)
  return ids.join(' ')
}

async function onSubmit(): Promise<void> {
  const auction = await form.submit()
  if (auction) emit('created', auction)
}
</script>

<template>
  <form class="flex flex-col gap-6" novalidate @submit.prevent="onSubmit">
    <fieldset class="grid min-w-0 gap-x-4 gap-y-3 sm:grid-cols-2">
      <legend class="mb-1 text-body font-bold">The car</legend>

      <div v-for="field in carFields" :key="field.name" :class="field.wide ? 'sm:col-span-2' : ''" class="flex flex-col gap-1">
        <label :for="field.name" class="font-semibold">{{ field.label }}</label>
        <p :id="hintId(field.name)" class="text-meta text-ink-muted">{{ field.hint }}</p>

        <textarea
          v-if="field.type === 'textarea'"
          :id="field.name"
          v-model="form.values[field.name]"
          class="field min-h-20 py-2"
          rows="3"
          :aria-invalid="form.visibleError(field.name) ? 'true' : undefined"
          :aria-describedby="describedBy(field.name)"
          @blur="form.touch(field.name)"
        />
        <input
          v-else
          :id="field.name"
          v-model="form.values[field.name]"
          :type="field.type"
          class="field"
          :aria-invalid="form.visibleError(field.name) ? 'true' : undefined"
          :aria-describedby="describedBy(field.name)"
          @blur="form.touch(field.name)"
        />

        <p v-if="form.visibleError(field.name)" :id="errorId(field.name)" class="text-meta text-danger" role="alert">
          {{ form.visibleError(field.name) }}
        </p>
      </div>
    </fieldset>

    <fieldset class="grid min-w-0 gap-x-4 gap-y-3 sm:grid-cols-2">
      <legend class="mb-1 text-body font-bold">The auction</legend>

      <div v-for="field in auctionFields" :key="field.name" :class="field.wide ? 'sm:col-span-2' : ''" class="flex flex-col gap-1">
        <label :for="field.name" class="font-semibold">{{ field.label }}</label>
        <p :id="hintId(field.name)" class="text-meta text-ink-muted">{{ field.hint }}</p>
        <input
          :id="field.name"
          v-model="form.values[field.name]"
          :type="field.type"
          class="field"
          :aria-invalid="form.visibleError(field.name) ? 'true' : undefined"
          :aria-describedby="describedBy(field.name)"
          @blur="form.touch(field.name)"
        />
        <p v-if="form.visibleError(field.name)" :id="errorId(field.name)" class="text-meta text-danger" role="alert">
          {{ form.visibleError(field.name) }}
        </p>
      </div>
    </fieldset>

    <div class="flex flex-wrap items-center gap-4">
      <button type="submit" class="btn btn-primary" :disabled="!form.isValid.value || form.submitting.value">
        {{ form.submitting.value ? 'Creating auction…' : 'Create auction' }}
      </button>
      <p v-if="form.failed.value" class="text-danger" role="alert">Could not create the auction. Try again.</p>
    </div>
  </form>
</template>
