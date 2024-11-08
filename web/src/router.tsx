import {Gav, ResolvedPackage, stringToGav} from './util.ts'
import {createBrowserRouter} from 'react-router-dom'
import {App} from './App.tsx'
import {ClientError, ErrorBoundary, NotFoundError} from './ErrorBoundary.tsx'
import {PackagePage} from './components/PackagePage.tsx'
import {ArtifactInfoContainer} from '@/components/ArtifactInfoContainer.tsx'

export interface PackageLoaderData {
  versions: string[]
  analyzedPackages: ResolvedPackage[]
}

const serverUrl = import.meta.env.VITE_SERVER_URL

const loadPackageData = async (gav: Gav): Promise<PackageLoaderData | Response> => {
  const queryString = `groupId=${gav.groupId}&artifactId=${gav.artifactId}`
  const versionsPromise = fetch(`${serverUrl}/api/v1/maven/versions?${queryString}`)
    .then(async res => {
      const json = await res.json()
      if (res.ok) {
        return (json as string[]).toReversed()
      } else if (res.status === 400) {
        throw new NotFoundError(`Package not found`)
      } else {
        throw Error(json.message)
      }
    })
  const analyzedPackagesPromise = fetch(`${serverUrl}/api/v1/packages?${queryString}`)
    .then(async res => {
      const json = await res.json()
      if (res.ok) {
        return json as ResolvedPackage[]
      } else {
        throw Error(json.message)
      }
    })

  return {versions: await versionsPromise, analyzedPackages: await analyzedPackagesPromise}
}

export const createRouter = () => createBrowserRouter([
  {
    path: '/',
    Component: App,
    children: [
      {
        index: true,
        element: null
      },
      {
        errorElement: <ErrorBoundary/>,
        path: '/packages/:gav',
        element: <PackagePage/>,
        loader: async ({params}) => {
          const gav = stringToGav(params.gav!)
          if (!gav.groupId || !gav.artifactId) {
            throw new ClientError('Package format is invalid')
          }
          return loadPackageData(gav)
        },
        shouldRevalidate: ({currentParams, nextParams}) => {
          const oldGav = stringToGav(currentParams.gav!)
          const newGav = stringToGav(nextParams.gav!)
          return oldGav.groupId !== newGav.groupId || oldGav.artifactId !== newGav.artifactId
        },
        children: [
          {
            errorElement: <ErrorBoundary/>,
            index: true,
            element: <ArtifactInfoContainer/>
          }
        ]
      },
      {
        path: '*',
        errorElement: <ErrorBoundary/>,
        loader: async () => {
          throw new NotFoundError('Resource not found')
        }
      }
    ]
  }
])