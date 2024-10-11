import {LoaderCircle} from 'lucide-react'
import {cn} from '@/lib/utils.ts'

export const LoadingSpinner = ({className, ...props}: any) => {
  return <LoaderCircle className={cn('rotating', className)} {...props}/>
}