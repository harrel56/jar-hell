import {useRouteError} from 'react-router-dom'

export const ErrorBoundary = () => {
  const err = useRouteError() as any
  console.error(err)
  const msg = 'message' in err ? err.message : 'Unknown error occurred'
  return <div>{msg}</div>
}