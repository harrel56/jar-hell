import {useOutlet} from 'react-router-dom'

export const App = () => {
  const outlet = useOutlet()

  return <div className='lg:w-[1000px] md:w-full m-auto p-12 flex flex-col items-center'>{outlet}</div>
}