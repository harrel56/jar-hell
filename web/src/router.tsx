import {createRouter, useNavigate} from '@solidjs/router'
import {PackagePage} from './pages/PackagePage'
import {parseGav} from './utils/gav'
import {ArtifactTree, getPackage, getVersions} from './api'

export interface PackageLoaderData {
  versions: string[],
  pkg: ArtifactTree
}

export const Router = createRouter({
  preloadLinks: false,
  routes: [
    { path: '/', component: () => <p>index</p> },
    {
      path: '/packages/:coordinate',
      matchFilters: {
        coordinate: (param) => {
          const gav = parseGav(param)
          return gav?.version !== undefined
        }
      },
      preload: async (args): Promise<PackageLoaderData> => {
        const coordinate = args.params['coordinate']!
        const gav = parseGav(coordinate)!
        const [versions, pkg] = await Promise.all([getVersions(gav.groupId, gav.artifactId, gav.classifier), getPackage(coordinate)])
        return { versions, pkg }
      },
      component: PackagePage,
    },
    {
      path: '/packages/:coordinate',
      matchFilters: {
        coordinate: (param) => {
          const gav = parseGav(param)
          return gav ? gav.version === undefined : false
        }
      },
      preload: async (args) => {
        const navigate = useNavigate()
        const coordinate = args.params['coordinate']!
        const gav = parseGav(coordinate)!
        const versions = await getVersions(gav.groupId, gav.artifactId, gav.classifier)
        navigate(`/packages/${coordinate}:${versions.at(-1)}`, {replace: true})
      }
    },
    { path: '*404', component: () => <p>not found</p> },
  ],
})