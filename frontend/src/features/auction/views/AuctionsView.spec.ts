import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AuctionsView from './AuctionsView.vue'
import { sampleAuctions } from '../data/sampleAuctions'

describe('AuctionsView', () => {
  it('shows the live auctions heading', () => {
    const wrapper = mount(AuctionsView)

    expect(wrapper.find('h1').text()).toBe('Live auctions')
  })

  it('renders one card for each sample auction', () => {
    const wrapper = mount(AuctionsView)

    expect(wrapper.findAll('.auction-card')).toHaveLength(sampleAuctions.length)
  })
})
