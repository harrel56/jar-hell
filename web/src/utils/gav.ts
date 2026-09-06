
export interface Gav {
  groupId: string
  artifactId: string
  version?: string | undefined
}

export function parseGav(data: string | undefined): Gav | null {
  if (!data) {
    return null
  }
  const parts = data.split(':')
  if (parts.length !== 2 && parts.length !== 3) {
    return null
  }
  return {
    groupId: parts[0]!,
    artifactId: parts[1]!,
    version: parts[2]
  }
}