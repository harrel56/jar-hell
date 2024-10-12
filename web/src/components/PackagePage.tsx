import {VersionPicker} from '@/components/VersionPicker.tsx'
import {useLoaderData, useParams} from 'react-router-dom'
import {PackageLoaderData} from '@/router.tsx'
import {Separator} from '@/shadcn/components/ui/Separator.tsx'
import {useFetch} from '@/hooks/useFetch.ts'
import {Package, stringToGav} from '@/util.ts'
import {useLayoutEffect, useMemo, useState} from 'react'
import {ByteCount} from '@/components/ByteCount.tsx'
import {PendingAnalysis} from '@/components/PendingAnalysis.tsx'
import {LoadingSpinner} from '@/components/LoadingSpinner.tsx'

export const PackagePage = () => {
  const loaderData = useLoaderData() as PackageLoaderData
  const { gav } = useParams()
  const gavObject = useMemo(() => stringToGav(gav!), [gav])
  const [analyzedPackages, setAnalyzedPackages] = useState(loaderData.analyzedPackages)
  const {data: getData, loading: getLoading, error: getError, get} = useFetch<Package>(`/api/v1/packages/${gav}?depth=1`, [gav])
  const {data: postData, loading: postLoading, error: postError, post} = useFetch<Package>('/api/v1/analyze-and-wait', [gav])
  const packageData = getData ?? postData
  const loading = getLoading ?? postLoading
  const error = getError ?? postError

  useLayoutEffect(() => {
    if (analyzedPackages.some(pkg => pkg.version === gavObject.version)) {
      get(`/api/v1/packages/${gav}?depth=1`)
    } else {
      post('/api/v1/analyze-and-wait', JSON.stringify(gavObject))
    }
  }, [gav])
  useLayoutEffect(() => {
    if (postData) {
      loaderData.analyzedPackages.push(postData)
      setAnalyzedPackages(pkgs => [...pkgs, postData])
    }
  }, [postData])
  return (
    <div className='flex basis-1 gap-4'>
      <VersionPicker versions={loaderData.versions} analyzedPackages={analyzedPackages}/>
      <Separator orientation='vertical' className='h-auto'/>
      <div className='min-w-[600px] min-h-[400px] w-full flex justify-center'>
        {getLoading && <LoadingSpinner/>}
        {postLoading && <PendingAnalysis/>}
        {error && <p className='text-destructive'>Error occurred: {error.data?.message}</p>}
        {packageData?.totalSize && !loading && !error && <ByteCount bytes={packageData.totalSize}/>}
      </div>
    </div>)
}