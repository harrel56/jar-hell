import {useRouteError} from 'react-router-dom'

export class NotFoundError extends Error {
  constructor(message: string) {
    super(message)
  }
}

export const ErrorBoundary = () => {
  const err = useRouteError() as any
  if (err instanceof NotFoundError) {
    return (
      <div>
        <h1>404</h1>
        <p>{err.message}</p>
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