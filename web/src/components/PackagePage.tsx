import {VersionPicker} from '@/components/VersionPicker.tsx'
import {useLoaderData} from 'react-router-dom'
import {PackageLoaderData} from '@/router.tsx'
import {Separator} from '@/shadcn/components/ui/Separator.tsx'
import {useFetch} from '@/hooks/useFetch.ts'
import {gavToString, Package} from '@/util.ts'
import {useLayoutEffect, useState} from 'react'
import {LoadingSpinner} from '@/components/LoadingSpinner.tsx'
import {ByteCount} from '@/components/ByteCount.tsx'
import {PendingAnalysis} from '@/components/PendingAnalysis.tsx'

export const PackagePage = () => {
  const loaderData = useLoaderData() as PackageLoaderData
  const [analyzedPackages, setAnalyzedPackages] = useState(loaderData.analyzedPackages)
  const {data, loading, error, post} = useFetch<Package>('/api/v1/analyze-and-wait')
  const packageData = loaderData.packageData ?? data

  useLayoutEffect(() => {
    if (!loaderData.packageData) {
      post('', JSON.stringify(loaderData.gav))
    }
    setAnalyzedPackages(loaderData.analyzedPackages)
  }, [gavToString(loaderData.gav)])
  useLayoutEffect(() => {
    if (data) {
      setAnalyzedPackages(pkgs => [...pkgs, data])
    }
  }, [data])

  return (
    <div className='flex basis-1 gap-4'>
      <VersionPicker versions={loaderData.versions} analyzedPackages={analyzedPackages}/>
      <Separator orientation='vertical' className='h-auto'/>
      <div className='min-w-[600px] min-h-[400px] w-full flex items-center justify-center'>
        {!loading && <PendingAnalysis/>}
        {error && <p>Error occurred</p>}
        {/*{packageData?.totalSize && !loading && !error && <ByteCount bytes={packageData.totalSize}/>}*/}
      </div>
    </div>)
}