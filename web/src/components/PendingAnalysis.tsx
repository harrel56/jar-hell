import {LoadingSpinner} from '@/components/LoadingSpinner.tsx'

export const PendingAnalysis = () => {
  return (
    <div className='flex flex-col items-center text-center'>
      <span className='text-2xl'>Analysis is in progress...</span>
      <LoadingSpinner className='w-16 h-16 my-6'/>
      <span className='text-faded'>Depending on a package it can take up to several minutes.</span>
      <span className='text-border'>If you believe that analysis is stuck, please file an <a href='https://github.com/harrel56/jar-hell/issues'>issue</a>.</span>
    </div>
  )
}