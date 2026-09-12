import { describe, expect, test } from 'vitest'
import { formatSize, formatSizeText } from './utils'

describe('formatSize', () => {
  test.each([
    [0, '0', 'bytes'],
    [1, '1', 'byte'],
    [2, '2', 'bytes'],
    [456, '456', 'bytes'],
    [999, '999', 'bytes'],
    [1000, '1.00', 'KB'],
    [1024, '1.02', 'KB'],
    [1500, '1.50', 'KB'],
    [64579, '64.58', 'KB'],
    [999_994, '999.99', 'KB'],
    [999_995, '1.00', 'MB'],
    [1_000_000, '1.00', 'MB'],
    [1_005_000, '1.00', 'MB'],
    [1_005_001, '1.01', 'MB'],
    [1_331_200, '1.33', 'MB'],
    [999_995_000, '1.00', 'GB'],
    [1_000_000_000, '1.00', 'GB'],
    [12_345_678_901, '12.35', 'GB'],
    [1_500_000_000_000, '1500.00', 'GB'],
    // invalid input is treated as zero
    [-1, '0', 'bytes'],
    [-1_000_000, '0', 'bytes'],
    [Number.NaN, '0', 'bytes'],
    [Number.POSITIVE_INFINITY, '0', 'bytes'],
    [Number.NEGATIVE_INFINITY, '0', 'bytes'],
  ])('%s -> %s %s', (bytes, value, unit) => {
    expect(formatSize(bytes)).toEqual({ value, unit })
  })
})

describe('formatSizeText', () => {
  test.each([
    [0, '0 bytes'],
    [456, '456 bytes'],
    [211_010, '211.01 KB'],
    [11_980_000, '11.98 MB'],
    [2_000_000_000, '2.00 GB'],
  ])('%i bytes → "%s"', (bytes, text) => {
    expect(formatSizeText(bytes)).toBe(text)
  })
})
