import type {BytecodeVersion} from '../api'

export interface FormattedSize {
  value: string
  unit: string
}

const size = (value: string, unit: string) => ({value, unit})

export const formatSize = (bytes: number): FormattedSize => {
  if (!Number.isFinite(bytes) || !Number.isInteger(bytes) || bytes <= 0) {
    return size(String(0), 'bytes')
  } else if (bytes < 1_000) {
    return size(String(bytes), bytes === 1 ? 'byte' : 'bytes')
  } else if (bytes < 999_995) {
    return size((bytes / 1_000).toFixed(2), 'KB')
  } else if (bytes < 999_995_000) {
    return size((bytes / 1_000_000).toFixed(2), 'MB')
  } else {
    return size((bytes / 1_000_000_000).toFixed(2), 'GB')
  }
}

export const formatSizeText = (bytes: number) => {
  const { value, unit } = formatSize(bytes)
  return `${value} ${unit}`
}

export const formatBytecodeVersion = (ver: BytecodeVersion): string => {
  if (ver.major === 45 && ver.minor === 3) {
    return '1.1'
  } else if (ver.major === 45) {
    return '1.0'
  } else if (ver.major === 46) {
    return '1.2'
  } else if (ver.major === 47) {
    return '1.3'
  } else if (ver.major === 48) {
    return '1.4'
  }

  const preview = ver.minor === 65535 ? ' (preview)': ''
  return (ver.major - 44) + preview
}
