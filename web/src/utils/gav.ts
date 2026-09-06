
export interface Gav {
  groupId: string
  artifactId: string
  version?: string
  classifier?: string
}

export function parseGav(data: string | undefined): Gav | null {
  if (!data) {
    return null
  }
  const parts = data.split(':')
  if (parts.length === 2) {
    return {
      groupId: parts[0]!,
      artifactId: parts[1]!
    }
  } else if (parts.length === 3) {
    return {
      groupId: parts[0]!,
      artifactId: parts[1]!,
      version: parts[2]!
    }
  } else if (parts.length === 4) {
    return {
      groupId: parts[0]!,
      artifactId: parts[1]!,
      version: parts[2]!,
      classifier: parts[3]!
    }
  } else {
    return null
  }
}