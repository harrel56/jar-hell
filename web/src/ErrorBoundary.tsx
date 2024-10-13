import {useRouteError} from 'react-router-dom'

export class NotFoundError extends Error {
  constructor(message: string) {
    super(message)
  }
}

export class ClientError extends Error {
  constructor(message: string) {
    super(message)
  }
}

export const ErrorBoundary = () => {
  const err = useRouteError() as any
  if (err instanceof NotFoundError) {
    return (
      <div className='flex flex-col gap-5 items-center'>
        <span className='text-7xl font-bold tracking-tighter text-border'>404</span>
        <p className='text-lg'>{err.message}</p>
      </div>
    )
  } else if (err instanceof ClientError) {
    return (
      <div className='flex flex-col gap-5 items-center'>
        <span className='text-7xl font-bold tracking-tighter text-border'>400</span>
        <p className='text-lg'>{err.message}</p>
      </div>
    )
  } else {
    return (
      <div>
        <p>{err.message ?? 'Unknown error occurred'}</p>
      </div>
    )
  }
}