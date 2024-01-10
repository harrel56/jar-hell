import {useOutlet} from 'react-router-dom'

export const App = () => {
  const outlet = useOutlet()

  return (
    <div className='w-full flex flex-col items-center'>
      <div className='w-8/12 flex flex-col items-center'>{outlet}</div>
    </div>)
}