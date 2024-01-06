import ReactDOMClient from 'react-dom/client'
import {Autocomplete} from './Autocomplete.tsx'
import {createBrowserRouter, RouterProvider} from 'react-router-dom'
import {App} from './App.tsx'
import {PackagePage} from './PackagePage.tsx'
import {Gav, gavToString, Package, stringToGav} from './util.ts'
import {ErrorBoundary} from './ErrorBoundary.tsx'

export interface PackageLoaderData {
  versions: string[]
  analyzedPackages: Package[]
  packageData: Package
}

const loadPackageData = async (gav: Gav): Promise<PackageLoaderData> => {
  const queryString = `groupId=${gav.groupId}&artifactId=${gav.artifactId}`
  const versionsPromise = fetch(`${import.meta.env.VITE_SERVER_URL}/api/v1/maven/versions?${queryString}`)
    .then(res => res.json())
  const analyzedPackagesPromise = fetch(`${import.meta.env.VITE_SERVER_URL}/api/v1/packages?${queryString}`)
    .then(res => res.json())
  const packageDataPromise = fetch(`${import.meta.env.VITE_SERVER_URL}/api/v1/packages/${gavToString(gav)}?depth=1`)
    .then(res => res.json())
  const joined = await Promise.all([versionsPromise, analyzedPackagesPromise, packageDataPromise])
  return { versions: joined[0], analyzedPackages: joined[1], packageData: joined[2] }
}

const router = createBrowserRouter([
  {
    path: '/',
    Component: App,
    children: [
      {
        index: true,
        element: <Autocomplete/>
      },
      {
        errorElement: <ErrorBoundary/>,
        path: '/packages/:gav',
        element: <PackagePage/>,
        loader: async ({params}) => {
          const gav = stringToGav(params.gav!)
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

const root = ReactDOMClient.createRoot(document.getElementById('root')!)
root.render(<RouterProvider router={router}/>
)