import ReactDOMClient from 'react-dom/client'
import {Autocomplete} from './Autocomplete.tsx'
import {createBrowserRouter, RouterProvider} from 'react-router-dom'
import {App} from './App.tsx'
import {PackagePage} from './PackagePage.tsx'

const router = createBrowserRouter([
  {
    path: '/',
    Component: App,
    children: [
      {
        index: true,
        element: <Autocomplete/>,
      },
      {
        path: '/packages/:gav',
        element: <PackagePage/>,
        loader: async ({ params }) => {
          return fetch(`${import.meta.env.VITE_SERVER_URL}/api/v1/packages/${params.gav}`);
        },
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