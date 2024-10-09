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
  return {groupId, artifactId: artifactId ?? groupId, version, classifier}
}

export interface License {
  name: string
  url: string
}

export interface Package extends Gav {
  unresolved?: boolean
  packageSize?: number
  totalSize?: number
  bytecodeVersion?: string
  packaging?: string
  name?: string
  description?: string
  url?: string
  inceptionYear?: string
  licenses: License[]
  classifiers: string[]
  created: string
}