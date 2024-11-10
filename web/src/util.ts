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

export interface License {
  name: string
  url: string
}

export type Package = UnresolvedPackage | ResolvedPackage

export interface UnresolvedPackage extends Gav {
  unresolved: true
}

export interface ResolvedPackage extends Gav {
  unresolved: false
  packageSize: number
  bytecodeVersion?: string
  packaging: string
  name?: string
  description?: string
  url?: string
  inceptionYear?: string
  licenses: License[]
  classifiers: string[]
  created: string
  analyzed: string
  dependencies: {
    artifact: Package
    scope: string
    optional: boolean
  }[]
  effectiveValues: {
    requiredDependencies: number
    optionalDependencies: number
    unresolvedDependencies: number
    size: number
    bytecodeVersion?: string
  }
}

export const isResolvedPackage = (pkg: Package): pkg is ResolvedPackage => !pkg.unresolved

export const getAllDepsCount =
  (pkg: ResolvedPackage) => pkg.effectiveValues.requiredDependencies + pkg.effectiveValues.optionalDependencies