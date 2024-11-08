import {CircleHelpIcon} from 'lucide-react'
import {Tooltip, TooltipContent, TooltipProvider, TooltipTrigger} from '@/shadcn/components/ui/Tooltip.tsx'

export interface ByteDisplayProps {
  title: string
  titleHint: string
  value: any
  valueHint: string
}

export const MetricDisplay = ({title, titleHint, value, valueHint}: ByteDisplayProps) => {
  return (
    <TooltipProvider>
      <div className='flex flex-col items-center gap-1'>
        <span className='text-xl font-bold'>
          {title}
          <Tooltip delayDuration={200}>
            <TooltipTrigger asChild>
              <CircleHelpIcon size={16} className='ml-2 inline-block align-baseline'/>
            </TooltipTrigger>
            <TooltipContent className='max-w-64 p-4 font-semibold'>
              <p>{titleHint}</p>
            </TooltipContent>
          </Tooltip>
        </span>
        <span className='text-5xl text-faded font-mono' title={valueHint}>{value}</span>
      </div>
    </TooltipProvider>
  )
}