import {VersionPicker} from '@/VersionPicker.tsx'
import {useLoaderData} from 'react-router-dom'
import {PackageLoaderData} from '@/router.tsx'
import {Separator} from '@/components/ui/Separator.tsx'
import {useFetch} from '@/hooks/useFetch.ts'
import {gavToString, Package} from '@/util.ts'
import {useLayoutEffect} from 'react'

export const PackagePage = () => {
  const loaderData = useLoaderData() as PackageLoaderData
  const {data, loading, error, post} = useFetch<Package>('/api/v1/analyze-and-wait')
  const packageData = loaderData.packageData ?? data

  useLayoutEffect(() => {
    if (!loaderData.packageData) {
      post('', JSON.stringify(loaderData.gav))
    }
  }, [gavToString(loaderData.gav)])

  return (
    <div className='flex basis-1 gap-4'>
      <VersionPicker versions={loaderData.versions}/>
      <Separator orientation='vertical' className='h-auto'/>
      <div className='min-w-[600px] min-h-[400px] w-full bg-input'>
        {packageData && !loading && !error && <h1>{packageData.totalSize}</h1>}
      </div>
    </div>)
}