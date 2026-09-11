import { Errored, Loading } from 'solid-js'
import TopBar from './components/TopBar'
import { ThrownErrorView } from './components/ErrorView'
import './App.css'
import {Router} from './router'

export default function App() {
  return (
    <Router>
      {props => {
        return (
          <>
            <TopBar/>
            <Errored fallback={err => <ThrownErrorView error={err()}/>}>
              <Loading fallback={<main class="px-4 py-12">Loading…</main>}>
                {props.children}
              </Loading>
            </Errored>
          </>
        )
      }}
    </Router>
  )
}
