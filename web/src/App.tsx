import { Loading } from 'solid-js'
import TopBar from './components/TopBar'
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
