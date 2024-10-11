
export interface ByteDisplayProps {
  bytes: number
}

export const ByteCount = ({bytes}: ByteDisplayProps) => {
  return <span className='text-5xl'>{calculateMsg(bytes)}</span>
}

const calculateMsg = (bytes: number) => {
  if (bytes < 1_000) {
    return bytes + 'B'
  }
  if (bytes < 1_000_000) {
    return (bytes / 1_000).toFixed(2) + 'KB'
  }
  if (bytes < 1_000_000_000) {
    return (bytes / 1_000_000).toFixed(2) + 'MB'
  } else {
    return (bytes / 1_000_000_000).toFixed(2) + 'GB'
  }
}