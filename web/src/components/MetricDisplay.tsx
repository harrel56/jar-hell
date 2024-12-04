import {CircleHelpIcon} from 'lucide-react'
import {Tooltip, TooltipContent, TooltipProvider, TooltipTrigger} from '@/shadcn/components/ui/Tooltip.tsx'
import {cn} from '@/shadcn/lib/utils.ts'

export interface ByteDisplayProps {
  className?: string
  title: string
  titleHint?: string
  value: any
  valueHint?: string
}

export const MetricDisplay = ({className, title, titleHint, value, valueHint}: ByteDisplayProps) => {
  return (
    <TooltipProvider>
      <div className={cn('flex flex-col items-center gap-1 whitespace-nowrap', className)}>
        <span className='text-xl font-bold text-center'>
          {title}
          {titleHint && (
            <Tooltip delayDuration={200}>
              <TooltipTrigger asChild>
                <CircleHelpIcon size={16} className='ml-2 inline-block align-baseline'/>
              </TooltipTrigger>
              <TooltipContent className='max-w-64 p-2 font-semibold whitespace-normal'>
                <p>{titleHint}</p>
              </TooltipContent>
            </Tooltip>
          )}
        </span>
        <span className='text-5xl text-faded font-mono' title={valueHint}>{value}</span>
      </div>
    </TooltipProvider>
  )
}