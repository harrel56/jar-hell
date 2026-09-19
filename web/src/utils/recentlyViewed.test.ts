import { beforeEach, describe, expect, test, vi } from 'vitest'
import { flush } from 'solid-js'
import { ArtifactInfo } from '../api'

/* this jsdom setup ships without web storage, so stub the two methods the module touches
   before it is imported (module-level read on import) */
vi.hoisted(() => {
  const store = new Map<string, string>()
  vi.stubGlobal('localStorage', {
    getItem: (key: string) => store.get(key) ?? null,
    setItem: (key: string, value: string) => store.set(key, value),
    clear: () => store.clear(),
  })
})
const { recentlyViewed, rememberViewed: remember } = await import('./recentlyViewed')

/* signal writes are batched, flush so the accessor reflects the write */
const rememberViewed = (info: ArtifactInfo) => {
  remember(info)
  flush()
}

const info = (artifactId: string, version = '1.0', extra: Partial<ArtifactInfo> = {}): ArtifactInfo => ({
  groupId: 'org.test',
  artifactId,
  version,
  packageSize: 1000,
  effectiveValues: { requiredDependencies: 1, optionalDependencies: 0, unresolvedDependencies: 0, size: 5000, licenseType: 'MIT', licenseTypes: [] },
  ...extra,
})

const withoutEffective = (artifactId: string): ArtifactInfo => {
  const { effectiveValues: _, ...rest } = info(artifactId)
  return rest
}

const ids = () => recentlyViewed().map(p => `${p.artifactId}:${p.version}`)

describe('recentlyViewed', () => {
  beforeEach(() => {
    localStorage.clear()
    // drain the module-level list through the public api
    for (let i = 0; i < 6; i++) rememberViewed(info(`drain${i}`))
  })

  test('stores the sizes needed to render a row', () => {
    rememberViewed(info('lib'))

    const [first] = recentlyViewed()
    expect(first).toEqual({ groupId: 'org.test', artifactId: 'lib', version: '1.0', packageSize: 1000, effectiveSize: 5000 })
    expect(JSON.parse(localStorage.getItem('recentlyViewed')!)[0]).toEqual(first)
  })

  test('falls back to the package size when effective values are missing', () => {
    rememberViewed(withoutEffective('lib'))

    expect(recentlyViewed()[0]!.effectiveSize).toBe(1000)
  })

  test('moves a revisited package to the front instead of duplicating it', () => {
    rememberViewed(info('a'))
    rememberViewed(info('b'))
    rememberViewed(info('a'))

    expect(ids().slice(0, 2)).toEqual(['a:1.0', 'b:1.0'])
    expect(ids().filter(id => id === 'a:1.0')).toHaveLength(1)
  })

  test('replaces the entry when another version or classifier of the same package is viewed', () => {
    rememberViewed(info('a', '1.0'))
    rememberViewed(info('b'))
    rememberViewed(info('a', '2.0', { classifier: 'jakarta' }))

    expect(ids().slice(0, 2)).toEqual(['a:2.0', 'b:1.0'])
    expect(recentlyViewed()[0]!.classifier).toBe('jakarta')
    expect(ids().filter(id => id.startsWith('a:'))).toHaveLength(1)
  })

  test('caps the list at six', () => {
    for (let i = 0; i < 8; i++) rememberViewed(info(`lib${i}`))

    expect(recentlyViewed()).toHaveLength(6)
    expect(ids()[0]).toBe('lib7:1.0')
  })
})
