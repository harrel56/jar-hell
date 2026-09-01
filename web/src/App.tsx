import { Title } from '@solidjs/meta';
import { Loading } from 'solid-js';
import './App.css';
import {createRouter} from '@solidjs/router'

const Router = createRouter({
  routes: [
    {
      path: '/index', component: () => <p>index</p>
    },
    {
      path: '*404', component:() =>  <p>not found</p>
    }
  ]
})

export default function App() {
  return (
    <Router>
      {(props) => (
        <>
          <Loading fallback={<main class="px-4 py-12">Loading…</main>}>
            {props.children}
          </Loading>
        </>
      )}
    </Router>
  );
}
