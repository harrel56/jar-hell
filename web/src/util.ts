export interface Gav {
  groupId: string
  artifactId: string
  version: string
  classifier?: string
}

export const gavToString = (gav: Gav) => {
  if (gav.classifier) {
    return `${gav.groupId}:${gav.artifactId}:${gav.version}:${gav.classifier}`
  } else {
    return `${gav.groupId}:${gav.artifactId}:${gav.version}`
  }
}

export const stringToGav = (str: string): Gav => {
  const [groupId, artifactId, version, classifier] = str.split(':', 4)
  return {groupId, artifactId, version, classifier}
}

export const gavEquals = (gav1: Gav, gav2: Gav) => {
  return gav1.groupId === gav2.groupId && gav1.artifactId === gav2.artifactId
    && gav1.version === gav2.version && gav1.classifier === gav2.classifier
}

export const formatBytes = (bytes: number) => {
  if (bytes < 1_000) {
    return bytes + 'B'
  }
  if (bytes < 1_000_000) {
    return (bytes / 1_000).toFixed(2) + 'KB'
  }
  if (bytes < 1_000_000_000) {
    return (bytes / 1_000_000).toFixed(2) + 'MB'
  } else {
    return (bytes / 1_000_000_000).toFixed(2) + 'GB'
  }
}

export const formatBytecodeVersion = (bytecodeVersion?: string) => {
  if (!bytecodeVersion) {
    return 'N/A'
  }
  switch (bytecodeVersion) {
    case '45.0': return 'Java 1.0'
    case '45.3': return 'Java 1.1'
    case '46.0': return 'Java 1.2'
    case '47.0': return 'Java 1.3'
    case '48.0': return 'Java 1.4'
  }

  const [major, minor] = bytecodeVersion.split('.')
  const version = parseInt(major) - 44
  const preview = parseInt(minor) === 65535
  return 'Java ' + version + (preview ? '*' : '')
}

export const formatLicenseTypesMap = (types: Record<string, number>[]) => {
  return types.map(type => Object.entries(type).map(entry =>
    `<strong>${formatLicenseType(entry[0])}:</strong> ${entry[1]}`)).join(',<br>')
}

export const formatPackageLicenseType = (pkg: ResolvedPackage) => {
  if (pkg.licenseTypes.length === 0) {
    return formatLicenseType('NO_LICENSE')
  } else if (pkg.licenseTypes.length === 1) {
    return formatLicenseType(pkg.licenseTypes[0])
  } else if (pkg.licenseTypes.length === 2) {
    return `${formatLicenseType(pkg.licenseTypes[0])} + ${pkg.licenseTypes.length - 1} other`
  } else {
    return `${formatLicenseType(pkg.licenseTypes[0])} + ${pkg.licenseTypes.length - 1} others`
  }
}

export const formatLicenseType = (type: string) => {
  switch (type) {
    case 'NO_LICENSE': return 'No license'
    case 'UNKNOWN': return 'Unknown'
    case 'SSPL_1': return 'SSPL 1.0'
    case 'CC0_1': return 'CC0 1.0'
    case 'UNLICENSE': return 'The Unlicense'
    case 'AGPL_3': return 'Affero GPL 3.0'
    case 'CDDL_1': return 'CDDL 1.0'
    case 'GPL_2': return 'GPL 2.0'
    case 'GPL_3': return 'GPL 3.0'
    case 'LGPL_2': return 'LGPL 2.1'
    case 'LGPL_3': return 'LGPL 3.0'
    case 'CPL_1': return 'CPL 1.0'
    case 'EPL_1': return 'Eclipse 1.0'
    case 'EPL_2': return 'Eclipse 2.0'
    case 'MPL_1': return 'Mozilla 1.0'
    case 'MPL_2': return 'Mozilla 2.0'
    case 'BSD_1': return 'BSD 1-clause'
    case 'BSD_3': return 'BSD 3-clause'
    case 'APACHE_2': return 'Apache 2.0'
    case 'BSD_2': return 'BSD 2-clause'
    case 'ICU': return 'ICU'
    case 'ZLIB': return 'ZLIB'
    case 'ISC': return 'ISC'
    case 'MIT': return 'MIT'
    case 'BSD_0': return 'BSD 0-clause'
    case 'MIT0': return 'MIT No Attribution'
    default: return type
  }
}

export interface License {
  name: string
  url: string
}

export type Package = UnresolvedPackage | ResolvedPackage

export interface UnresolvedPackage extends Gav {
  unresolved: true
}

export interface Dependency {
  artifact: Package
  scope: string
  optional: boolean
}

export interface ResolvedPackage extends Gav {
  unresolved: false
  packageSize: number
  bytecodeVersion?: string
  packaging: string
  name?: string
  description?: string
  url?: string
  scmUrl?: string
  issuesUrl?: string
  inceptionYear?: string
  licenses: License[]
  licenseTypes: string[]
  classifiers: string[]
  created: string
  analyzed: string
  dependencies: Dependency[]
  effectiveValues: {
    requiredDependencies: number
    optionalDependencies: number
    unresolvedDependencies: number
    size: number
    bytecodeVersion?: string
    licenseType: string
    licenseTypes: Record<string, number>[]
  }
}

export const isResolvedPackage = (pkg: Package): pkg is ResolvedPackage => !pkg.unresolved

export const getAllDepsCount =
  (pkg: ResolvedPackage) => pkg.effectiveValues.requiredDependencies + pkg.effectiveValues.optionalDependencies

export const formatDate = (date: string)=> {
  return new Date(date).toLocaleDateString('en-GB', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
}

export const formatDateTime = (date: string)=> {
  return new Date(date).toLocaleDateString('en-GB', {
    year: 'numeric',
    month: 'long',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
}