import { Verdict, verdict } from './VerdictPill'

const KB = 1000
const MB = 1000 * KB

export interface Band extends Verdict {
  chevrons: string
}

const band = (label: string, token: string): Band =>
  ({ ...verdict(label, token), chevrons: `var(--chevrons-${token})` })

const SIZE_BANDS = [
  { upTo: 512 * KB, tickLabel: '512KB', ...band('Light', 'good') },
  { upTo: 4 * MB, tickLabel: '4MB', ...band('Moderate', 'warn') },
  { upTo: 20 * MB, tickLabel: '20MB', ...band('Heavy', 'bad') },
  { upTo: Infinity, tickLabel: '', ...band('Very heavy', 'critical') },
]

export const sizeBand = (bytes: number): Band => SIZE_BANDS.find(b => bytes < b.upTo)!

const RAIL_MIN = 50 * KB
const RAIL_MAX = 50 * MB
export const railPosition = (bytes: number) =>
  Math.max(2, Math.min(100, Math.log10(Math.max(bytes, RAIL_MIN) / RAIL_MIN) / Math.log10(RAIL_MAX / RAIL_MIN) * 100))

export const RAIL_TICKS = SIZE_BANDS
  .filter(b => Number.isFinite(b.upTo))
  .map(b => ({ label: b.tickLabel, left: `${railPosition(b.upTo)}%` }))
