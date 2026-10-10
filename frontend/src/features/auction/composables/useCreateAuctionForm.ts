import { computed, reactive, ref } from 'vue'
import { createAuction } from '../api'
import {
  EMPTY_AUCTION_FORM,
  toCreateAuctionRequest,
  validateAuctionForm,
  type AuctionFormField,
  type AuctionFormValues,
} from '../auctionForm'
import type { AuctionResponse } from '../types'

export function useCreateAuctionForm(now: () => Date = () => new Date()) {
  const values = reactive<AuctionFormValues>({ ...EMPTY_AUCTION_FORM })
  const touched = reactive<Partial<Record<AuctionFormField, boolean>>>({})
  const submitAttempted = ref(false)
  const submitting = ref(false)
  const failed = ref(false)

  const errors = computed(() => validateAuctionForm(values, now()))
  const isValid = computed(() => Object.keys(errors.value).length === 0)

  function touch(field: AuctionFormField): void {
    touched[field] = true
  }

  function visibleError(field: AuctionFormField): string | undefined {
    return touched[field] || submitAttempted.value ? errors.value[field] : undefined
  }

  async function submit(): Promise<AuctionResponse | null> {
    submitAttempted.value = true
    if (!isValid.value || submitting.value) return null

    submitting.value = true
    failed.value = false
    try {
      return await createAuction(toCreateAuctionRequest(values))
    } catch {
      failed.value = true
      return null
    } finally {
      submitting.value = false
    }
  }

  return { values, touch, visibleError, isValid, submitting, failed, submit }
}
