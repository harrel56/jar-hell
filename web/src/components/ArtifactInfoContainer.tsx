import {useOutletContext, useParams} from 'react-router-dom'
import {stringToGav} from '@/util.ts'
import {useMemo} from 'react'
import {OutletContext} from '@/components/PackagePage.tsx'
import {ArtifactInfo} from '@/components/ArtifactInfo.tsx'
import {NotFoundError} from '@/ErrorBoundary.tsx'

export const ArtifactInfoContainer = () => {
  const ctx = useOutletContext<OutletContext>()
  const {gav} = useParams()
  const gavObject = useMemo(() => stringToGav(gav!), [gav])

  if (!gavObject.version) {
    return null
  }
  if (!ctx.versions.includes(gavObject.version)) {
    throw new NotFoundError(`Version ${gavObject.version} not found`)
  }
  return <ArtifactInfo/>
}