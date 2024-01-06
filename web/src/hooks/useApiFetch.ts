import {CachePolicies, useFetch} from 'use-http'

export const useApiFetch = <T>(path: string) => {
  return useFetch<T>(import.meta.env.VITE_SERVER_URL + path, {cachePolicy: CachePolicies.NO_CACHE})
}