import {useLoaderData} from 'react-router-dom'
import {Autocomplete} from './Autocomplete.tsx'
import {PackageLoaderData} from './index.tsx'

export const PackagePage = () => {
  const data = useLoaderData() as PackageLoaderData
  console.log(data)

  return (
    <div>
      <p>{JSON.stringify(data)}</p>
      <Autocomplete/>
    </div>)
}