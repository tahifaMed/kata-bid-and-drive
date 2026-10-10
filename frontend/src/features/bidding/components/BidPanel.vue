<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ApiError } from '../../../api/client'
import { formatCurrency } from '../../auction/format'
import type { AuctionResponse } from '../../auction/types'
import { useIdentityStore } from '../../identity/stores/identity'
import { placeBid } from '../api'
import { toCents } from '../money'

const props = defineProps<{ auction: AuctionResponse }>()
const emit = defineEmits<{ placed: [] }>()

const identity = useIdentityStore()

const amount = ref('')
const submitting = ref(false)
const outcome = ref<{ kind: 'placed' | 'failed'; text: string } | null>(null)

const isSeller = computed(() => props.auction.sellerId === identity.currentUserId)
const isHighestBidder = computed(() => props.auction.highestBidderId === identity.currentUserId)

const minimumCents = computed(() => toCents(props.auction.minNextBid) ?? 0)
const amountCents = computed(() => toCents(amount.value))

/** Shown while typing, so it is described by the input rather than announced. */
const amountHint = computed(() => {
  if (amount.value === '') return null
  if (amountCents.value === null)
    return 'Enter an amount such as 12000.00, with at most two decimals.'
  if (amountCents.value < minimumCents.value) {
    return `Your bid must be at least ${formatCurrency(props.auction.minNextBid)}.`
  }
  return null
})

const canSubmit = computed(
  () => !submitting.value && amountCents.value !== null && amountCents.value >= minimumCents.value,
)

// A message about the previous user's bid would mislead the next one.
watch(
  () => identity.currentUserId,
  () => {
    outcome.value = null
  },
)

function failureText(error: unknown): string {
  if (!(error instanceof ApiError)) return 'Could not place the bid. Try again.'
  const problem = error.problem
  if (error.status === 422 && typeof problem?.minimumBid === 'string') {
    return `Your bid must be at least ${formatCurrency(problem.minimumBid)}.`
  }
  if (error.status === 409 && problem?.title === 'Bid conflict') {
    return 'Another bid was placed first. Check the new minimum and try again.'
  }
  if ((error.status === 409 || error.status === 422) && problem?.detail) {
    return problem.detail
  }
  if (error.status === 400 && Array.isArray(problem?.errors)) {
    const first: unknown = problem.errors[0]
    if (
      typeof first === 'object' &&
      first !== null &&
      'message' in first &&
      typeof first.message === 'string'
    ) {
      return `Amount ${first.message}.`
    }
  }
  return 'Could not place the bid. Try again.'
}

async function onSubmit(): Promise<void> {
  const href = props.auction._links['place-bid']?.href
  if (!canSubmit.value || !href) return
  submitting.value = true
  outcome.value = null
  // The result belongs to the user who sent the bid, not to one switched in while it was on its way.
  const bidder = identity.currentUserId
  const stillSameUser = () => identity.currentUserId === bidder
  try {
    const bid = await placeBid(href, amount.value)
    amount.value = ''
    if (stillSameUser()) {
      outcome.value = {
        kind: 'placed',
        text: `Your bid of ${formatCurrency(bid.amount)} was placed.`,
      }
    }
    emit('placed')
  } catch (error) {
    if (stillSameUser()) {
      outcome.value = { kind: 'failed', text: failureText(error) }
    }
    // The auction moved on (closed, outbid, higher minimum), so the page fetches it again.
    if (error instanceof ApiError && (error.status === 409 || error.status === 422)) {
      emit('placed')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="stack">
    <p v-if="isSeller">You are the seller of this auction.</p>

    <p v-else-if="isHighestBidder" class="font-semibold text-success">
      You are the highest bidder.
    </p>

    <form v-else class="flex flex-col gap-3" novalidate @submit.prevent="onSubmit">
      <p id="bid-minimum">Minimum bid: {{ formatCurrency(auction.minNextBid) }}</p>
      <div class="flex flex-col gap-1">
        <label for="bid-amount" class="font-semibold">Your bid (USD)</label>
        <input
          id="bid-amount"
          v-model="amount"
          type="text"
          inputmode="decimal"
          autocomplete="off"
          class="field"
          :aria-invalid="amountHint ? 'true' : undefined"
          :aria-describedby="amountHint ? 'bid-minimum bid-amount-hint' : 'bid-minimum'"
        />
        <p v-if="amountHint" id="bid-amount-hint" class="text-meta text-ink-muted">
          {{ amountHint }}
        </p>
      </div>
      <div>
        <button type="submit" class="btn btn-primary" :disabled="!canSubmit">
          {{ submitting ? 'Placing bid…' : 'Place bid' }}
        </button>
      </div>
    </form>

    <p v-if="outcome?.kind === 'placed'" role="status" class="text-success">{{ outcome.text }}</p>
    <p v-else-if="outcome?.kind === 'failed'" role="alert" class="text-danger">
      {{ outcome.text }}
    </p>
  </div>
</template>
