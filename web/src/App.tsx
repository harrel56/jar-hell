import {useOutlet} from 'react-router-dom'

export const App = () => {
  const outlet = useOutlet()

  return (
    <div className='main-container'>
      <p>Hello there</p>
      <div>{outlet}</div>
    </div>)
}