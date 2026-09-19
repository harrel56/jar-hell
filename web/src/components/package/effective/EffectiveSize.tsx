import { createMemo, For, Show } from 'solid-js'
import { formatSize, formatSizeText } from '../../../utils/utils'
import { VerdictPill } from './VerdictPill'
import { RAIL_TICKS, railPosition, sizeBand } from './sizeRail'

const bytesTitle = (bytes: number) => `${bytes.toLocaleString('en-US')} bytes`

interface EffectiveSizeProps {
  selfBytes: number
  totalBytes: number
}

export function EffectiveSize(props: EffectiveSizeProps) {
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
          <VerdictPill verdict={band()}/>
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
