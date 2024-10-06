import {useOutlet} from 'react-router-dom'
import {ThemeProvider} from '@/components/ThemeProvider.tsx'
import {ThemeToggle} from '@/components/ThemeToggle.tsx'

export const App = () => {
  const outlet = useOutlet()
  return (
    <ThemeProvider>
      <div className='p-4 flex flex-col'>
        <div className='flex justify-end gap-2 max-w-[1400px] w-full self-center'>
          <ThemeToggle/>
        </div>
        {outlet}
      </div>
    </ThemeProvider>
  )
}