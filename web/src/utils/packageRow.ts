import { ArtifactInfo } from '../api'
import { Gav } from './gav'

export interface PackageRow extends Gav {
  version: string
  packageSize: number
  effectiveSize: number
}

export const toPackageRow = (info: ArtifactInfo): PackageRow => ({
  groupId: info.groupId,
  artifactId: info.artifactId,
  version: info.version,
  ...(info.classifier ? { classifier: info.classifier } : {}),
  packageSize: info.packageSize ?? 0,
  effectiveSize: info.effectiveValues?.size ?? info.packageSize ?? 0,
})
