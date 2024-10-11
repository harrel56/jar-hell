import {ThemeToggle} from '@/shadcn/components/ThemeToggle.tsx'
import {Icons} from '@/shadcn/components/ui/Icons.tsx'
import {Button} from '@/shadcn/components/ui/Button.tsx'
import {Link} from 'react-router-dom'
import imgUrl from '@static/jarhell.png'

export const NavBar = () => {
  return (
    <div className='flex justify-between max-w-[1400px] w-full py-4 self-center'>
      <Link className='flex items-center' to='/'>
        <img src={imgUrl} alt='hell in a jar' className='h-[40px] mr-4'></img>
        <span className='text-2xl font-extrabold capitalize'>
          <span>jar</span><span className='text-hellyeah'>hell</span>
        </span>
      </Link>
      <div className='flex'>
        <a href='https://github.com/harrel56/jar-hell' target='_blank'>
          <Button variant='ghost' size='icon'>
            <Icons.GitHub className='h-[1.2rem] w-[1.2rem]'/>
          </Button>
        </a>
        <ThemeToggle/>
      </div>
    </div>
  )
}