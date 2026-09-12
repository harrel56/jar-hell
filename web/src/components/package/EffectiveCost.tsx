import { createMemo, For, Show } from 'solid-js'
import { ArtifactInfo, EffectiveValues } from '../../api'
import { formatSize, formatSizeText } from '../../utils/utils'

interface EffectiveCostProps {
  info: ArtifactInfo
  effective: EffectiveValues
}

export function EffectiveCost(props: EffectiveCostProps) {
  return (
    <section class="mt-(--space-11) overflow-hidden rounded-(--radius-section) border border-(--hairline)">
      <div class="flex flex-wrap items-center gap-x-3 gap-y-2.5 border-b border-(--hairline) bg-(--surface) px-[22px] py-3">
        <span class="h-[13px] w-[3px] shrink-0 rounded-[2px] bg-(--accent)"/>
        <span class="whitespace-nowrap text-[12px] font-semibold uppercase tracking-(--tracking-caps) text-(--ink-4)">Effective cost</span>
        <span class="whitespace-nowrap text-(length:--text-meta) text-(--ink-5)">real resolved graph</span>
      </div>
      <div class="grid gap-px bg-(--hairline)">
        <EffectiveSize selfBytes={props.info.packageSize ?? 0} totalBytes={props.effective.size}/>
      </div>
    </section>
  )
}

const KB = 1000
const MB = 1000 * KB

interface Band {
  label: string
  color: string
  chevrons: string
}

const band = (label: string, token: string): Band =>
  ({ label, color: `var(--${token})`, chevrons: `var(--chevrons-${token})` })

const sizeBand = (bytes: number): Band => {
  if (bytes < 512 * KB) return band('Light', 'good')
  if (bytes < 4 * MB) return band('Moderate', 'warn')
  if (bytes < 20 * MB) return band('Heavy', 'bad')
  return band('Very heavy', 'critical')
}

// Rail is log-scaled from 50 KB to 20 MB
const RAIL_MIN = 50 * KB
const RAIL_MAX = 20 * MB
const railPosition = (bytes: number) =>
  Math.max(2, Math.min(100, Math.log10(Math.max(bytes, RAIL_MIN) / RAIL_MIN) / Math.log10(RAIL_MAX / RAIL_MIN) * 100))

const bytesTitle = (bytes: number) => `${bytes.toLocaleString('en-US')} bytes`

const RAIL_TICKS = [
  { label: '100KB', bytes: 100 * KB },
  { label: '1MB', bytes: MB },
  { label: '10MB', bytes: 10 * MB },
].map(tick => ({ label: tick.label, left: `${railPosition(tick.bytes)}%` }))

interface EffectiveSizeProps {
  selfBytes: number
  totalBytes: number
}

function EffectiveSize(props: EffectiveSizeProps) {
  const depsBytes = createMemo(() => Math.max(props.totalBytes - props.selfBytes, 0))
  const band = createMemo(() => sizeBand(props.totalBytes))
  const size = createMemo(() => formatSize(props.totalBytes))
  const hasDeps = () => depsBytes() > 0

  return (
    <div class="bg-(--ground) px-6 pt-[26px] pb-6">
      <div class="flex flex-wrap items-end gap-4">
        <div class="flex items-baseline gap-[9px] whitespace-nowrap font-(family-name:--font-data)" title={bytesTitle(props.totalBytes)}>
          <span class="text-(length:--text-metric-xl) font-medium leading-(--leading-metric) tracking-(--tracking-metric)">{size().value}</span>
          <span class="text-[24px] text-(--ink-3)">{size().unit}</span>
        </div>
        <div class="flex items-center gap-2.5 pb-[5px]">
          <span class="text-[15px] font-semibold">Effective size</span>
          <span class="rounded-(--radius-pill) px-2 py-[3px] text-(length:--text-label) font-semibold uppercase tracking-[0.05em] text-white"
                style={{ background: band().color }}>
            {band().label}
          </span>
        </div>
      </div>

      <div class="relative mt-[22px] h-3.5 rounded-(--radius-bar) bg-(--track)">
        <For each={RAIL_TICKS}>
          {tick => <div class="absolute -top-1 -bottom-1 w-px bg-(--hairline-tick)" style={{ left: tick.left }}/>}
        </For>
        <div class="absolute inset-y-0 left-0 rounded-(--radius-bar)"
             style={{ width: `${railPosition(props.totalBytes)}%`, background: band().chevrons }}/>
        <Show when={props.selfBytes > 0}>
          <div class={['absolute inset-y-0 left-0', hasDeps() ? 'rounded-l-(--radius-bar) shadow-[2px_0_0_var(--track)]' : 'rounded-(--radius-bar)']}
               style={{ width: `${railPosition(props.selfBytes)}%`, background: band().color }}/>
        </Show>
      </div>
      <div class="relative mt-1.5 h-3.5 font-(family-name:--font-data) text-(length:--text-micro) text-(--ink-5)">
        <For each={RAIL_TICKS}>
          {tick => <span class="absolute -translate-x-1/2" style={{ left: tick.left }}>{tick.label}</span>}
        </For>
      </div>

      <div class="mt-3 flex flex-wrap gap-x-4 gap-y-1.5 text-(length:--text-meta) text-(--ink-3)">
        <span class="flex items-center gap-[7px]" title={bytesTitle(props.selfBytes)}>
          <span class="size-2.5 shrink-0 rounded-[3px]" style={{ background: band().color }}/>
          package {formatSizeText(props.selfBytes)}
        </span>
        <span class={['flex items-center gap-[7px]', { 'text-(--ink-5)': !hasDeps() }]} title={bytesTitle(depsBytes())}>
          <span class="size-[9px] shrink-0 rounded-[2px]" style={{ background: hasDeps() ? band().chevrons : 'var(--hairline-strong)' }}/>
          transitive {formatSizeText(depsBytes())}
        </span>
      </div>
    </div>
  )
}
