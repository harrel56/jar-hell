import {useOutlet} from 'react-router-dom'
import {ThemeProvider} from '@/components/ThemeProvider'
import {NavBar} from '@/NavBar.tsx'
import {Autocomplete} from '@/Autocomplete.tsx'

export const App = () => {
  const outlet = useOutlet()
  return (
    <ThemeProvider>
      <div className='p-4 flex flex-col'>
        <NavBar/>
        <Autocomplete/>
        {outlet}
      </div>
    </ThemeProvider>
  )
}