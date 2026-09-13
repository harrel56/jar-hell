import { query, revalidate } from '@solidjs/router'
import {Gav} from './utils/gav'

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
  scmUrl?: string
  issuesUrl?: string
  /** ISO local date-times, e.g. `2026-02-27T12:34:56` */
  created?: string
  analyzed?: string
  licenseTypes?: string[]
  classifiers?: string[]
  extensions?: string[]
  jarInfo?: JarInfo
  effectiveValues?: EffectiveValues
  unresolved?: boolean
  unresolvedReason?: string
}

export interface BytecodeVersion {
  major: number,
  minor: number
}

export interface JarContent {
  count: number
  size: number
  compressedSize: number
}

export interface JarInfo {
  contents: Record<string, JarContent>
  publicClasses: Record<string, number>
  nonPublicClasses: number
  bytecodeVersion?: BytecodeVersion
  buildJdk?: string
  multiReleaseJar: boolean
  executable: boolean
  services: string[]
  moduleType: 'NAMED' | 'AUTOMATIC' | 'UNNAMED'
  moduleName?: string
}

export interface EffectiveValues {
  requiredDependencies: number
  unresolvedDependencies: number
  optionalDependencies: number
  size: number
  bytecodeVersion?: BytecodeVersion
  licenseType: string
  licenseTypes: Record<string, number>[]
}

export interface ArtifactTree {
  artifactInfo: ArtifactInfo
  dependencies?: DependencyInfo[] | null
}

export interface DependencyInfo {
  artifact: ArtifactTree
  optional: boolean
  scope: string
}

export class HttpError extends Error {
  constructor(readonly status: number, message: string) {
    super(message)
    this.name = 'HttpError'
  }
}

interface ErrorResponse {
  url: string
  method: string
  message: string
}

const json = async <T>(path: string, method = 'get', body: BodyInit | null = null): Promise<T> => {
  const res = await fetch(path, {method, body})
  if (!res.ok) {
    const message = await res.json().then((err: ErrorResponse) => err.message, () => res.statusText)
    throw new HttpError(res.status, message || `${method.toUpperCase()} ${path} failed with ${res.status}`)
  }
  return res.json()
}

export const getPackage = query(
  (coordinate: string) => json<ArtifactTree>(`/api/v1/packages/${coordinate}`), 'getPackage')

export const getVersions = query((groupId: string, artifactId: string, classifier: string | undefined) => {
  const classifierPart = classifier ? `?classifier=${encodeURIComponent(classifier)}` : ''
  return json<ArtifactVersion[]>(`/api/v1/packages/${groupId}:${artifactId}/versions${classifierPart}`)
}, 'getVersions')

export const analyzePackage = query(async (gav: Gav) => {
  const tree = await json<ArtifactTree>(`/api/v1/analyze-and-wait`, 'post', JSON.stringify(gav))
  revalidate(getVersions.keyFor(gav.groupId, gav.artifactId, gav.classifier))
  return tree
}, 'analyzePackage')
