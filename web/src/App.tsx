import {useOutlet} from 'react-router-dom'
import {ThemeProvider} from '@/components/ThemeProvider'
import {NavBar} from '@/NavBar.tsx'
import {Autocomplete} from '@/Autocomplete.tsx'

export const App = () => {
  const outlet = useOutlet()
  return (
    <ThemeProvider>
      <div className='px-4 flex flex-col'>
        <NavBar/>
        <Autocomplete/>
        <div className='max-w-[1400px] w-full self-center pt-12'>
          {outlet}
        </div>
      </div>
    </ThemeProvider>
  )
}