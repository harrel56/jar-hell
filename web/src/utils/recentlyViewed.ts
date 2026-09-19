import { createSignal } from 'solid-js'
import { ArtifactInfo } from '../api'
import { PackageRow, toPackageRow } from './packageRow'

export type RecentPackage = PackageRow

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

const [recent, setRecent] = createSignal<RecentPackage[]>(read())

export const recentlyViewed = recent

export const rememberViewed = (info: ArtifactInfo) => {
  const entry = toPackageRow(info)
  setRecent(prev => {
    const next = [entry, ...prev.filter(p => p.groupId !== entry.groupId || p.artifactId !== entry.artifactId)].slice(0, LIMIT)
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    } catch {
      // storage may be full or disabled, the in-memory list still works for this session
    }
    return next
  })
}
