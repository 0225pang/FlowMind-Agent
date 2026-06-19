import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import StatCard from '@/components/StatCard.vue'

describe('StatCard — Stats display component', () => {
  it('should render label, value and trend', () => {
    const wrapper = mount(StatCard, {
      props: {
        label: '文档',
        value: 10,
        trend: '本周 +2',
        icon: 'Collection',
        color: '#5b6cff'
      }
    })

    expect(wrapper.text()).toContain('文档')
    expect(wrapper.text()).toContain('10')
    expect(wrapper.text()).toContain('本周 +2')
  })

  it('should render numeric value', () => {
    const wrapper = mount(StatCard, {
      props: { label: 'Count', value: 42, trend: '+5', icon: 'Collection', color: '#19b37b' }
    })
    expect(wrapper.text()).toContain('42')
  })

  it('should render string value', () => {
    const wrapper = mount(StatCard, {
      props: { label: 'Status', value: 'Active', trend: 'OK', icon: 'Collection', color: '#f59e0b' }
    })
    expect(wrapper.text()).toContain('Active')
  })

  it('should render percentage value', () => {
    const wrapper = mount(StatCard, {
      props: { label: 'Rate', value: '92%', trend: '+3%', icon: 'Collection', color: '#ef4444' }
    })
    expect(wrapper.text()).toContain('92%')
  })

  it('should apply color style to icon', () => {
    const wrapper = mount(StatCard, {
      props: { label: 'Test', value: 1, trend: '', icon: 'Collection', color: '#ff0000' }
    })
    expect(wrapper.html()).toContain('#ff0000')
  })

  it('should be visible (not empty)', () => {
    const wrapper = mount(StatCard, {
      props: { label: 'Visible', value: 100, trend: '', icon: 'Collection', color: '#000' }
    })
    expect(wrapper.element.innerHTML).toBeTruthy()
  })
})
