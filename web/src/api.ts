import { query } from '@solidjs/router'

export interface ArtifactVersion {
  version: string,
  analyzed: boolean
}

export interface ArtifactInfo {
  groupId: string
  artifactId: string
  version: string
  classifier: string
  name?: string
  description?: string
  packaging?: string
  packageSize?: number
  url?: string
  licenseTypes?: string[]
  unresolved?: boolean
}

export interface ArtifactTree {
  artifactInfo: ArtifactInfo
  dependencies: DependencyInfo[] | null
}

export interface DependencyInfo {
  artifact: ArtifactTree
  optional: boolean
  scope: string
}

const json = async <T>(path: string): Promise<T> => {
  const res = await fetch(path)
  if (!res.ok) {
    throw new Error(`${path} failed with ${res.status}`)
  }
  return res.json()
}

export const getPackage = query(
  (coordinate: string) => json<ArtifactTree>(`/api/v1/packages/${coordinate}`), 'getPackage')

export const getVersions = query((groupId: string, artifactId: string, classifier: string | undefined) => {
  const classifierPart = classifier ? `?classifier=${encodeURIComponent(classifier)}` : ''
  return json<ArtifactVersion[]>(`/api/v1/packages/${groupId}:${artifactId}/versions${classifierPart}`)
}, 'getVersions')
