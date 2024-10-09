import {VersionPicker} from '@/VersionPicker.tsx'
import {useLoaderData} from 'react-router-dom'
import {PackageLoaderData} from '@/router.tsx'

export const PackagePage = () => {
  const data = useLoaderData() as PackageLoaderData

  return (
    <div>
      <VersionPicker versions={data.versions}/>
    </div>)
}