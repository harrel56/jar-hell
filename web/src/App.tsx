import { Loading } from 'solid-js'
import { createRouter } from '@solidjs/router'
import TopBar from './components/TopBar'
import './App.css'

const Router = createRouter({
  routes: [
    { path: '/', component: () => <p>index</p> },
    { path: '*404', component: () => <p>not found</p> },
  ],
})

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
