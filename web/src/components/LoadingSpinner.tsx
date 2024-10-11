import {LoaderCircle} from 'lucide-react'
import {cn} from '@/lib/utils.ts'
import * as React from 'react'

export const LoadingSpinner = ({className, ...props}: React.ButtonHTMLAttributes<SVGElement>) => {
  return <LoaderCircle className={cn('rotating', className)} {...props}/>
}