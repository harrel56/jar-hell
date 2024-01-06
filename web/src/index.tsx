import ReactDOMClient from 'react-dom/client'
import {RouterProvider} from 'react-router-dom'
import {createRouter} from './router.tsx'

const root = ReactDOMClient.createRoot(document.getElementById('root')!)
root.render(<RouterProvider router={createRouter()}/>
)