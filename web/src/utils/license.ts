export type LicenseKind = 'very-permissive' | 'permissive' | 'weak-copyleft' | 'copyleft' | 'unknown'

export const formatLicenseType = (type: string): [string, LicenseKind] => {
  switch (type) {
    case 'UNKNOWN': return ['unknown', 'unknown']
    case 'NO_LICENSE': return ['No license', 'copyleft']
    case 'SSPL_1': return ['SSPL 1.0', 'copyleft']
    case 'CC0_1': return ['CC0 1.0', 'copyleft']
    case 'UNLICENSE': return ['The Unlicense', 'copyleft']
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
    case 'ICU': return ['ICU', 'very-permissive']
    case 'ZLIB': return ['ZLIB', 'very-permissive']
    case 'ISC': return ['ISC', 'very-permissive']
    case 'MIT': return ['MIT', 'very-permissive']
    case 'BSD_0': return ['BSD 0-clause', 'very-permissive']
    case 'MIT0': return ['MIT No Attribution', 'very-permissive']
    default: return [type, 'unknown']
  }
}
