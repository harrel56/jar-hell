import {useOutletContext, useParams} from 'react-router-dom'
import {useFetch} from '@/hooks/useFetch.ts'
import {Package, stringToGav} from '@/util.ts'
import {useLayoutEffect, useMemo} from 'react'
import {ByteCount} from '@/components/ByteCount.tsx'
import {PendingAnalysis} from '@/components/PendingAnalysis.tsx'
import {LoadingSpinner} from '@/components/LoadingSpinner.tsx'
import {OutletContext} from '@/components/PackagePage.tsx'

export const ArtifactInfo = () => {
  const ctx = useOutletContext<OutletContext>()
  const { gav } = useParams()
  const gavObject = useMemo(() => stringToGav(gav!), [gav])
  const {data: getData, loading: getLoading, error: getError, get} = useFetch<Package>(`/api/v1/packages/${gav}?depth=1`, [gav])
  const {data: postData, loading: postLoading, error: postError, post} = useFetch<Package>('/api/v1/analyze-and-wait', [gav])
  const packageData = getData ?? postData
  const loading = getLoading ?? postLoading
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
  if (packageData?.totalSize && !loading && !error) {
    return (
      <ByteCount bytes={packageData.totalSize}/>
    )
  }
}