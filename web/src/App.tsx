import {useOutlet} from 'react-router-dom'
import {ThemeProvider} from '@/components/ThemeProvider'
import {ThemeToggle} from '@/components/ThemeToggle'
import {Icons} from '@/components/ui/Icons'
import {Button} from '@/components/ui/Button.tsx'

export const App = () => {
  const outlet = useOutlet()
  return (
    <ThemeProvider>
      <div className='p-4 flex flex-col'>
        <div className='flex justify-end max-w-[1400px] w-full self-center'>
          <a href='https://github.com/harrel56/jar-hell' target='_blank'>
            <Button variant='ghost' size='icon'>
              <Icons.GitHub className="h-[1.2rem] w-[1.2rem]" />
            </Button>
          </a>
          <ThemeToggle/>
        </div>
        {outlet}
      </div>
    </ThemeProvider>
  )
}