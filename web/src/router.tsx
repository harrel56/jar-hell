import {Gav, gavToString, Package, stringToGav} from './util.ts'
import {createBrowserRouter, redirect} from 'react-router-dom'
import {App} from './App.tsx'
import {ErrorBoundary} from './ErrorBoundary.tsx'
import {PackagePage} from './PackagePage.tsx'

export interface PackageLoaderData {
  versions: string[]
  analyzedPackages: Package[]
  packageData: Package | null
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

  let packageDataPromise = Promise.resolve<Package | null>(null)
  const versions = await versionsPromise
  const analyzedPackages = await analyzedPackagesPromise
  if (gav.version) {
    if (!versions.includes(gav.version)) {
      return redirect(`/packages/${gav.groupId}:${gav.artifactId}`)
    }
    if (analyzedPackages.some(pkg => pkg.version === gav.version)) {
      packageDataPromise = fetch(`${serverUrl}/api/v1/packages/${gavToString(gav)}?depth=1`)
        .then(async res => {
          if (res.ok) {
            return res.json()
          } else if (res.status === 404) {
            return null
          } else {
            const json = await res.json()
            throw Error(json.message)
          }
        })
    }
  }
  const joined = await Promise.all([versionsPromise, analyzedPackagesPromise, packageDataPromise])
  return {versions: versions, analyzedPackages: joined[1], packageData: joined[2]}
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
            throw Error("Package format is invalid")
          }
          return loadPackageData(gav)
        }
      },
      {
        path: '*',
        element: <h1>whoopsy daisy here nothing</h1>
      }
    ]
  }
])