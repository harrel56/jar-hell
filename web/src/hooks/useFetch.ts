import { type Accessor, createSignal, onCleanup } from 'solid-js'

export interface FetchError {
  status?: number
  cause?: unknown
}

export interface Fetcher<T> {
  data: Accessor<T | undefined>
  loading: Accessor<boolean>
  error: Accessor<FetchError | undefined>
  get: (path: string) => Promise<void>
  post: (path: string, body?: BodyInit) => Promise<void>
  /** Drops any in-flight request and clears data/error. */
  reset: () => void
}

export function useFetch<T>(): Fetcher<T> {
  const [data, setData] = createSignal<T>()
  const [loading, setLoading] = createSignal(false)
  const [error, setError] = createSignal<FetchError>()

  let abortController: AbortController | undefined

  onCleanup(() => abortController?.abort())

  const request = async (method: string, path: string, body?: BodyInit) => {
    abortController?.abort()
    abortController = new AbortController()
    setLoading(true)
    setError(undefined)
    try {
      const res = await fetch(path, { method, body, signal: abortController.signal })
      if (res.ok) {
        setData(await res.json())
      } else {
        setData(undefined)
        setError({ status: res.status })
      }
    } catch (e) {
      if (abortController.signal.aborted) {
        return
      }
      setData(undefined)
      setError({ cause: e })
    } finally {
      if (!abortController.signal.aborted) {
        setLoading(false)
      }
    }
  }

  return {
    data,
    loading,
    error,
    get: path => request('get', path),
    post: (path, body) => request('post', path, body),
    reset: () => {
      abortController?.abort()
      abortController = undefined
      setData(undefined)
      setError(undefined)
      setLoading(false)
    },
  }
}
