import {useLoaderData} from 'react-router-dom'
import {PackageLoaderData} from './router.tsx'

export const PackagePage = () => {
  const data = useLoaderData() as PackageLoaderData
  console.log(data)

  return (
    <div>
      <ul>
        {data.versions.map(version => (
          <li>{version}</li>
        ))}
      </ul>
    </div>)
}