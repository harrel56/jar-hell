import * as React from 'react'

import {cn} from '@/shadcn/lib/utils.ts'

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  StartIcon?: React.ComponentType<any>
  EndIcon?: React.ComponentType<any>
}

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({className, type, StartIcon, EndIcon, ...props}, ref) => {
    return (
      <div className='w-full relative flex items-center'>
        {StartIcon && <StartIcon className='absolute left-4 w-8 h-8 text-foreground text-center transition-all disabled:pointer-events-none disabled:opacity-50'/>}
        <input
          type={type}
          className={cn('flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-lg ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50',
            StartIcon && 'pl-14',
            EndIcon && 'pr-14',
            className
          )}
          ref={ref}
          {...props}
        />
        {EndIcon && <EndIcon className='absolute right-4 w-8 h-8 text-foreground text-center transition-all disabled:pointer-events-none disabled:opacity-50'/>}
      </div>
    )
  }
)
Input.displayName = 'Input'

export {Input}
