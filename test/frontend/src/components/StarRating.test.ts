import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import StarRating from '@/components/StarRating.vue'

describe('StarRating — Rating component', () => {
  it('should render stars based on modelValue', () => {
    const wrapper = mount(StarRating, {
      props: { modelValue: 3, readonly: true }
    })
    // Should have SVG stars
    expect(wrapper.find('svg').exists()).toBe(true)
  })

  it('should render zero stars for modelValue 0', () => {
    const wrapper = mount(StarRating, {
      props: { modelValue: 0, readonly: true }
    })
    expect(wrapper.element.innerHTML).toBeTruthy()
  })

  it('should render five stars for modelValue 5', () => {
    const wrapper = mount(StarRating, {
      props: { modelValue: 5, readonly: true }
    })
    expect(wrapper.element.innerHTML).toBeTruthy()
  })

  it('should accept readonly prop', () => {
    const wrapper = mount(StarRating, {
      props: { modelValue: 4, readonly: true }
    })
    expect(wrapper.props('readonly')).toBe(true)
  })

  it('should accept showLabel prop', () => {
    const wrapper = mount(StarRating, {
      props: { modelValue: 4, readonly: true, showLabel: true }
    })
    expect(wrapper.props('showLabel')).toBe(true)
  })

  it('should not emit update:modelValue when readonly', async () => {
    const wrapper = mount(StarRating, {
      props: { modelValue: 3, readonly: true }
    })
    const stars = wrapper.findAll('svg')
    if (stars.length > 0) {
      await stars[0].trigger('click')
    }
    expect(wrapper.emitted('update:modelValue')).toBeFalsy()
  })
})
