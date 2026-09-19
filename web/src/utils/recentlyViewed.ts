import { createSignal } from 'solid-js'
import { ArtifactInfo } from '../api'
import { Gav } from './gav'

export interface RecentPackage extends Gav {
  version: string
  packageSize: number
  effectiveSize: number
}

const STORAGE_KEY = 'recentlyViewed'
const LIMIT = 6

const isRecentPackage = (value: unknown): value is RecentPackage => {
  const v = value as Partial<RecentPackage> | null
  return typeof v === 'object' && v !== null
    && typeof v.groupId === 'string' && typeof v.artifactId === 'string' && typeof v.version === 'string'
    && typeof v.packageSize === 'number' && typeof v.effectiveSize === 'number'
}

const read = (): RecentPackage[] => {
  try {
    const parsed: unknown = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]')
    return Array.isArray(parsed) ? parsed.filter(isRecentPackage) : []
  } catch {
    return []
  }
}

// plain list is the source of truth: signal writes are batched, so back-to-back
// remembers would otherwise each start from the same stale value
let list = read()
const [recent, setRecent] = createSignal<RecentPackage[]>(list)

export const recentlyViewed = recent

export const rememberViewed = (info: ArtifactInfo) => {
  const entry: RecentPackage = {
    groupId: info.groupId,
    artifactId: info.artifactId,
    version: info.version,
    ...(info.classifier ? { classifier: info.classifier } : {}),
    packageSize: info.packageSize ?? 0,
    effectiveSize: info.effectiveValues?.size ?? info.packageSize ?? 0,
  }
  // one row per package: the most recently viewed version (and classifier) replaces the previous one
  list = [entry, ...list.filter(p => p.groupId !== entry.groupId || p.artifactId !== entry.artifactId)].slice(0, LIMIT)
  setRecent(list)
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(list))
  } catch {
    // storage may be full or disabled, the in-memory list still works for this session
  }
}
