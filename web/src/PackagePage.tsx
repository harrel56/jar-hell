import {useLoaderData, useParams} from 'react-router-dom'
import {Autocomplete} from './Autocomplete.tsx'

export const PackagePage = () => {
  const { gav } = useParams()
  const data = useLoaderData()

  return (
    <div>
      <p>{JSON.stringify(data)}</p>
      <p>{gav}</p>
      <Autocomplete/>
    </div>)
}