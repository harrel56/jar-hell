import {LoadingSpinner} from '@/components/LoadingSpinner.tsx'
import {useEffect, useState} from 'react'
import {clsx} from 'clsx'

export const PendingAnalysis = () => {
  const [msg1Visible, setMsg1Visible] = useState(false)
  const [msg2Visible, setMsg2Visible] = useState(false)
  useEffect(() => {
    const id = setTimeout(() => setMsg1Visible(true), 6_000)
    return () => clearTimeout(id)
  }, [])
  useEffect(() => {
    const id = setTimeout(() => setMsg2Visible(true), 25_000)
    return () => clearTimeout(id)
  }, [])

  return (
    <div className='flex flex-col items-center text-center'>
      <span className='text-2xl'>Analysis is in progress...</span>
      <LoadingSpinner className='w-16 h-16 my-6'/>
      <span className={clsx('transition-opacity duration-1000 text-faded opacity-0', msg1Visible && 'opacity-100')}>
        Depending on a package it can take up to several minutes.
      </span>
      <span className={clsx('transition-opacity duration-1000 text-input text-sm opacity-0', msg2Visible && 'opacity-100')}>
        If you believe that analysis is stuck, please file an <a className='text-blue-dim' target='_blank' href='https://github.com/harrel56/jar-hell/issues'>issue</a>.
      </span>
    </div>
  )
}