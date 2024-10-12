import {Gav, Package, stringToGav} from './util.ts'
import {createBrowserRouter, redirect} from 'react-router-dom'
import {App} from './App.tsx'
import {ErrorBoundary} from './ErrorBoundary.tsx'
import {PackagePage} from './components/PackagePage.tsx'

export interface PackageLoaderData {
  versions: string[]
  analyzedPackages: Package[]
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
        throw Error(`Artifact not found`)
      } else {
        throw Error(json.message)
      }
    })
  const analyzedPackagesPromise = fetch(`${serverUrl}/api/v1/packages?${queryString}`)
    .then(async res => {
      const json = await res.json()
      if (res.ok) {
        return json as Package[]
      } else {
        throw Error(json.message)
      }
    })

  const versions = await versionsPromise
  if (gav.version && !versions.includes(gav.version)) {
    return redirect(`/packages/${gav.groupId}:${gav.artifactId}`)
  }
  return {versions, analyzedPackages: await analyzedPackagesPromise}
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
          if (!gav) {
            throw Error('Package format is invalid')
          }
          return loadPackageData(gav)
        },
        shouldRevalidate: ({currentParams, nextParams}) => {
          const oldGav = stringToGav(currentParams.gav!)
          const newGav = stringToGav(nextParams.gav!)
          return oldGav.groupId !== newGav.groupId || oldGav.artifactId !== newGav.artifactId
        }
      },
      {
        path: '*',
        element: <h1>whoopsy daisy here nothing</h1>
      }
    ]
  }
])