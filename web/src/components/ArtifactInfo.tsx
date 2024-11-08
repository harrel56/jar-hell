import {useOutletContext, useParams} from 'react-router-dom'
import {useFetch} from '@/hooks/useFetch.ts'
import {ResolvedPackage, stringToGav} from '@/util.ts'
import {useLayoutEffect, useMemo} from 'react'
import {MetricDisplay} from '@/components/MetricDisplay.tsx'
import {PendingAnalysis} from '@/components/PendingAnalysis.tsx'
import {LoadingSpinner} from '@/components/LoadingSpinner.tsx'
import {OutletContext} from '@/components/PackagePage.tsx'
import {formatBytes} from '@/lib/utils.ts'

export const ArtifactInfo = () => {
  const ctx = useOutletContext<OutletContext>()
  const { gav } = useParams()
  const gavObject = useMemo(() => stringToGav(gav!), [gav])
  const {data: getData, loading: getLoading, error: getError, get} = useFetch<ResolvedPackage>(`/api/v1/packages/${gav}?depth=1`, [gav])
  const {data: postData, loading: postLoading, error: postError, post} = useFetch<ResolvedPackage>('/api/v1/analyze-and-wait', [gav])
  const packageData = getData ?? postData
  const error = getError ?? postError

  useLayoutEffect(() => {
    if (ctx.analyzedPackages.some(pkg => pkg.version === gavObject.version)) {
      get(`/api/v1/packages/${gav}?depth=1`)
    } else {
      post('/api/v1/analyze-and-wait', JSON.stringify(gavObject))
    }
  }, [gav])
  useLayoutEffect(() => {
    if (postData) {
      ctx.markAsAnalyzed(postData)
    }
  }, [postData])

  if (getLoading) {
    return <LoadingSpinner/>
  }
  if (postLoading) {
    return <PendingAnalysis/>
  }
  if (error) {
    return <p className='text-destructive'>Error occurred: {error.data?.message}</p>
  }
  if (!packageData) {
    return null
  }
  if (packageData.unresolved) {
    return <p>This package couldn't be analyzed</p>
  }

  const metrics = [
    {
      title: 'Total size',
      titleHint:
        `The total (estimated) size of the package with all its required dependencies.
        Optional dependencies are not included.
        The resulting installation cost might be lower if some of the package's dependencies are already installed.`,
      value: formatBytes(packageData?.effectiveValues.size),
      valueHint: packageData?.effectiveValues.size + ' bytes'
    }
  ]
  return (
    <MetricDisplay {...metrics[0]}/>
  )
}