import {CircleHelpIcon} from 'lucide-react'

export interface ByteDisplayProps {
  title: string
  titleHint: string
  value: any
  valueHint: string
}

export const MetricDisplay = ({title, titleHint, value, valueHint}: ByteDisplayProps) => {
  return (
    <div className='flex flex-col items-center gap-1'>
      <span className='text-xl font-bold' title={titleHint}>{title} <CircleHelpIcon size={16} className='inline-block align-baseline'/></span>
      <span className='text-5xl text-faded font-mono' title={valueHint}>{value}</span>
    </div>
  )
}