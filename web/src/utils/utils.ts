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

export type LicenseKind = 'permissive' | 'weak-copyleft' | 'copyleft' | 'risky' | 'unusable' | 'unknown'

export const formatLicenseType = (type: string): [string, LicenseKind] => {
  switch (type) {
    case 'UNKNOWN': return ['Unknown', 'unknown']
    case 'NO_LICENSE': return ['No license', 'unusable']
    case 'SSPL_1': return ['SSPL 1.0', 'copyleft']
    case 'CC0_1': return ['CC0 1.0', 'risky']
    case 'UNLICENSE': return ['The Unlicense', 'risky']
    case 'AGPL_3': return ['Affero GPL 3.0', 'copyleft']
    case 'CDDL_1': return ['CDDL 1.0', 'copyleft']
    case 'GPL_2': return ['GPL 2.0', 'copyleft']
    case 'GPL_3': return ['GPL 3.0', 'copyleft']
    case 'LGPL_2': return ['LGPL 2.1', 'weak-copyleft']
    case 'LGPL_3': return ['LGPL 3.0', 'weak-copyleft']
    case 'CPL_1': return ['CPL 1.0', 'weak-copyleft']
    case 'EPL_1': return ['Eclipse 1.0', 'weak-copyleft']
    case 'EPL_2': return ['Eclipse 2.0', 'weak-copyleft']
    case 'MPL_1': return ['Mozilla 1.0', 'weak-copyleft']
    case 'MPL_2': return ['Mozilla 2.0', 'weak-copyleft']
    case 'BSD_1': return ['BSD 1-clause', 'permissive']
    case 'BSD_3': return ['BSD 3-clause', 'permissive']
    case 'APACHE_2': return ['Apache 2.0', 'permissive']
    case 'BSD_2': return ['BSD 2-clause', 'permissive']
    case 'ICU': return ['ICU', 'permissive']
    case 'ZLIB': return ['ZLIB', 'permissive']
    case 'ISC': return ['ISC', 'permissive']
    case 'MIT': return ['MIT', 'permissive']
    case 'BSD_0': return ['BSD 0-clause', 'permissive']
    case 'MIT0': return ['MIT No Attribution', 'permissive']
    default: return [type, 'unknown']
  }
}

