import {createRouter, type RoutePreloadFuncArgs, useNavigate} from '@solidjs/router'
import PackagePage from './pages/PackagePage'
import {parseGav} from './utils/gav'
import {getPackage, getVersions} from './api'
import {newestVersion} from './utils/compareVersions'

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
      preload: ({ params }: RoutePreloadFuncArgs) => getPackage(params['coordinate']!),
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
      preload: async ({ params }: RoutePreloadFuncArgs) => {
        const navigate = useNavigate()
        const coordinate = params['coordinate']!
        const gav = parseGav(coordinate)!
        const versions = await getVersions(gav)
        const newest = newestVersion(versions.map(v => v.artifactInfo.version))
        navigate(`/packages/${coordinate}:${newest}`, {replace: true})
      }
    },
    { path: '*404', component: () => <p>not found</p> },
  ],
})