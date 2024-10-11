import {useCallback, useLayoutEffect, useMemo, useState} from 'react'

export type Method = 'get' | 'post' | 'put' | 'delete' | 'patch' | 'head'
export type Body = BodyInit
export interface ErrorContext {
  status?: number
  cause?: any
  data?: {
    url: string
    method: Method
    message: string
  }
}

export const useFetch = <T = any>(userUri: string, deps: any[] = []) => {
  const [data, setData] = useState<T>()
  const [error, setError] = useState<ErrorContext>()
  const [loading, setLoading] = useState(false)
  const uri = useMemo(() => new URL(userUri, import.meta.env.VITE_SERVER_URL || document.baseURI), [userUri])
  useLayoutEffect(() => {
    setData(undefined)
    setError(undefined)
    setLoading(false)
  }, deps)

  const doFetch = async (method: Method, path: string, body?: Body) => {
    const href = new URL(path, uri).href
    setData(undefined)
    setError(undefined)
    setLoading(true)
    try {
      const res = await fetch(href, {
        method: method,
        body: body
      })
      const json = await res.json()
      if (res.ok) {
        setData(json)
      } else {
        setError({ status: res.status, data: json })
      }
    } catch (e) {
      setError({ cause: e })
    }
    setLoading(false)
  }

  const get = useCallback(async (path: string) => doFetch('get', path), [uri])
  const post = useCallback(async (path: string, body?: Body) => doFetch('post', path, body), [uri])

  return {
    data, error, loading, get, post
  }
}