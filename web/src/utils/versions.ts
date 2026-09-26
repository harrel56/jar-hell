import { ArtifactVersion } from '../api'
import { Gav } from './gav'

export const versionHref = (gav: Gav, version: string) => {
  const classifierPart = gav.classifier ? `:${gav.classifier}` : ''
  return `/packages/${gav.groupId}:${gav.artifactId}:${version}${classifierPart}`
}

/**
 * Group by minor version if:
 * - there is only 1 major
 * - in scope of 1 major there is a minor that got >= 10 patch versions
 * otherwise group by major
 * */
export const calculateVersionNodes = (versions: ArtifactVersion[]) => {
  const byMajor = new Map<string, Map<string, ArtifactVersion[]>>()
  versions.forEach(av => {
    const [major, minor] = av.version.split('.', 2)
    const byMinor = byMajor.get(major!) ?? new Map<string, ArtifactVersion[]>()
    const patches = byMinor.get(minor!) ?? []
    patches.push(av)
    byMinor.set(minor!, patches)
    byMajor.set(major!, byMinor)
  })

  const nodes = new Map<string, ArtifactVersion[]>()
  Array.from(byMajor.entries()).forEach(([major, byMinor]) => {
    const expandMinor = byMajor.size === 1 || Array.from(byMinor.values()).some(patches => patches.length >= 10)
    if (expandMinor) {
      Array.from(byMinor.entries()).forEach(([minor, patches]) => {
        nodes.set(`${major}.${minor}`, patches)
      })
    } else {
      const newVersions: ArtifactVersion[] = []
      Array.from(byMinor.values()).forEach(patches => newVersions.push(...patches))
      nodes.set(major, newVersions)
    }
  })
  return nodes
}
