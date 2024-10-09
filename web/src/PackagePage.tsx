import {VersionPicker} from '@/VersionPicker.tsx'
import {useLoaderData} from 'react-router-dom'
import {PackageLoaderData} from '@/router.tsx'
import {Separator} from '@/components/ui/Separator.tsx'

export const PackagePage = () => {
  const data = useLoaderData() as PackageLoaderData

  return (
    <div className='flex basis-1 gap-4'>
      <VersionPicker versions={data.versions}/>
      <Separator orientation='vertical' className='h-auto'/>
      <div className='min-w-[600px] min-h-[400px] w-full bg-input'></div>
    </div>)
}