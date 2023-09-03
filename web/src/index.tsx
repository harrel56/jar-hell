import ReactDOMClient from 'react-dom/client'
import {Autocomplete} from './Autocomplete.tsx'

const root = ReactDOMClient.createRoot(document.getElementById('root')!)
root.render(<Autocomplete></Autocomplete>)