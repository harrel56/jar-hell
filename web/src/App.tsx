import { Loading } from 'solid-js'
import {createRouter, defineRoute, useParams} from '@solidjs/router'
import TopBar from './components/TopBar'
import PackagePage, { loadPackage } from './pages/PackagePage'
import PackageRedirect, { loadNewestVersion } from './pages/PackageRedirect'
import './App.css'
import {Router} from './router'

export default function App() {
  return (
    <Router>
      {props => (
        <>
          <TopBar/>
          <Loading fallback={<main class="px-4 py-12">Loading…</main>}>
            {props.children}
          </Loading>
        </>
      )}
    </Router>
  )
}
