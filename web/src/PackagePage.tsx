import {VersionPicker} from '@/VersionPicker.tsx'
import {useLoaderData} from 'react-router-dom'
import {PackageLoaderData} from '@/router.tsx'
import {Separator} from '@/components/ui/Separator.tsx'
import {useFetch} from '@/hooks/useFetch.ts'
import {gavToString, Package} from '@/util.ts'
import {useLayoutEffect, useState} from 'react'
import {LoadingSpinner} from '@/LoadingSpinner.tsx'

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
      <div className='min-w-[600px] min-h-[400px] w-full flex bg-input items-center justify-center'>
        {loading && <LoadingSpinner/>}
        {error && <p>Error occurred</p>}
        {packageData && !loading && !error && <p className='text-5xl'>{packageData.totalSize}</p>}
      </div>
    </div>)
}