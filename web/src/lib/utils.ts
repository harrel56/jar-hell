import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export const formatBytes = (bytes: number) => {
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
