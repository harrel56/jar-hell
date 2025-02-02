import {createContext, PropsWithChildren, useEffect, useState} from 'react'
import {Gav, stringToGav} from '@/util.ts'
import {useParams} from 'react-router-dom'

export interface RecentlyViewedData {
  recentlyViewed: Gav[]
}

export const RecentlyViewedContext = createContext<RecentlyViewedData>({recentlyViewed: []})

export const RecentlyViewedProvider = ({children}: PropsWithChildren) => {
  const [recentlyViewed, setRecentlyViewed] = useState(() => JSON.parse(localStorage.getItem('recentlyViewed') ?? '[]') as Gav[])
  const {gav} = useParams()
  const gavObject = gav ? stringToGav(gav) : null
  useEffect(() => {
    if (gavObject && gavObject.version) {
      const newArray = recentlyViewed.filter(item => item.groupId !== gavObject.groupId || item.artifactId !== gavObject.artifactId)
      newArray.unshift(gavObject)
      if (newArray.length > 10) {
        newArray.pop()
      }
      localStorage.setItem('recentlyViewed', JSON.stringify(newArray))
      setRecentlyViewed(newArray)
    }
    
    if (gav) {
      document.title = 'Jar Hell | ' + gav
    } else {
      document.title = 'Jar Hell'
    }
  }, [gav])

  return (
    <RecentlyViewedContext value={{recentlyViewed}}>
      {children}
    </RecentlyViewedContext>
  )
}