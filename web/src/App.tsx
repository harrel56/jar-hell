import {useLocation, useOutlet} from 'react-router-dom'
import {ThemeProvider} from '@/shadcn/components/ThemeProvider'
import {NavBar} from '@/components/NavBar.tsx'
import {Autocomplete} from '@/components/Autocomplete.tsx'
import {useEffect} from 'react'
import {Footer} from '@/components/Footer.tsx'
import {RecentlyViewedProvider} from '@/context/RecentlyViewedProvider.tsx'

export const App = () => {
  const outlet = useOutlet()
  const { pathname } = useLocation()

  useEffect(() => window.scrollTo(0, 0), [pathname])

  return (
    <ThemeProvider>
      <RecentlyViewedProvider>
        <div className='px-4 flex flex-col'>
          <NavBar/>
          <Autocomplete/>
          <div className='max-w-[1400px] w-full self-center pt-12'>
            {outlet}
          </div>
        </div>
        <Footer/>
      </RecentlyViewedProvider>
    </ThemeProvider>
  )
}