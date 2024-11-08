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

export interface License {
  name: string
  url: string
}

export interface Package extends Gav{
  unresolved: boolean
}

export interface UnresolvedPackage extends Package {
  unresolved: true
}

export interface ResolvedPackage extends Package {
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
  dependencies: Package[]
  effectiveValues: {
    requiredDependencies: number
    optionalDependencies: number
    unresolvedDependencies: number
    size: number
    bytecodeVersion?: string
  }
}