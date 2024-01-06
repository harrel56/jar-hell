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
  const parts = str.split(':')
  if (parts.length === 3) {
    return {
      groupId: parts[0],
      artifactId: parts[1],
      version: parts[2]
    }
  } else if (parts.length === 4) {
    return {
      groupId: parts[0],
      artifactId: parts[1],
      version: parts[2],
      classifier: parts[3]
    }
  } else {
    throw Error('Invalid package coordinate')
  }
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