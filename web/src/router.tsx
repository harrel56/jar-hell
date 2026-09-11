import {createMemo} from 'solid-js'
import {createRouter, RouteSectionProps, useNavigate} from '@solidjs/router'
import {PackagePage} from './pages/PackagePage'
import {parseGav} from './utils/gav'
import {getVersions, HttpError} from './api'
import {ErrorView} from './components/ErrorView'

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
        if (versions.length === 0) {
          throw new HttpError(404, 'No versions found for ' + coordinate)
        }
        navigate(`/packages/${coordinate}:${versions.at(-1)?.version}`, {replace: true})
      },
      // The router hands the preload promise only to the route component - without one, a rejection is
      // unobservable. Reading it from a memo turns it into an async computation the App boundaries see.
      component: (props: RouteSectionProps<Promise<void>>) => {
        const redirect = createMemo(() => props.data)
        return <>{redirect()}</>
      },
    },
    { path: '*404', component: () => <ErrorView code={404} title='Page not found' details='How did you end up here?'/> },
  ],
})