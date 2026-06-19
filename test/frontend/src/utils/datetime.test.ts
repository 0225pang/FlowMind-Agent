import { describe, it, expect } from 'vitest'
import { fmtBeijing, relativeTime } from '@/utils/datetime'

describe('fmtBeijing — Beijing time formatter', () => {
  it('should format short date string (backend format)', () => {
    const result = fmtBeijing('2026-06-15 21:05:30')
    expect(result).toContain('06-15')
  })

  it('should return empty string for null', () => {
    expect(fmtBeijing(null)).toBe('')
  })

  it('should return empty string for undefined', () => {
    expect(fmtBeijing(undefined)).toBe('')
  })

  it('should return empty string for empty string', () => {
    expect(fmtBeijing('')).toBe('')
  })

  it('should handle ISO date string', () => {
    const result = fmtBeijing('2026-06-15T14:30:00Z')
    expect(result).toBeTruthy()
    expect(result.length).toBeGreaterThan(0)
  })

  it('should handle date-only string', () => {
    const result = fmtBeijing('2026-06-15')
    expect(result).toBeTruthy()
  })
})

describe('relativeTime — time ago formatter', () => {
  it('should return empty string for null', () => {
    expect(relativeTime(null)).toBe('')
  })

  it('should return empty string for empty string', () => {
    expect(relativeTime('')).toBe('')
  })

  it('should return "刚刚" for time within last minute', () => {
    const now = new Date()
    const result = relativeTime(now.toISOString())
    expect(result).toBe('刚刚')
  })

  it('should return "X分钟前" for recent times', () => {
    const fiveMinAgo = new Date(Date.now() - 5 * 60 * 1000)
    const result = relativeTime(fiveMinAgo.toISOString())
    expect(result).toMatch(/分钟前/)
  })

  it('should return "X小时前" for hours ago', () => {
    const twoHoursAgo = new Date(Date.now() - 2 * 3600 * 1000)
    const result = relativeTime(twoHoursAgo.toISOString())
    expect(result).toMatch(/小时前/)
  })

  it('should return "X天前" for days ago', () => {
    const threeDaysAgo = new Date(Date.now() - 3 * 86400 * 1000)
    const result = relativeTime(threeDaysAgo.toISOString())
    expect(result).toMatch(/天前/)
  })

  it('should format date for older timestamps', () => {
    const monthAgo = new Date(Date.now() - 30 * 86400 * 1000)
    const result = relativeTime(monthAgo.toISOString())
    expect(result).toBeTruthy()
    expect(result.length).toBeGreaterThan(0)
  })

  it('should handle invalid date string gracefully', () => {
    const result = relativeTime('not-a-date')
    expect(result).toBeTruthy()
    expect(typeof result).toBe('string')
  })
})
