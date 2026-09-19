import { For, Show } from 'solid-js'
import { RecentPackage, recentlyViewed } from '../../utils/recentlyViewed'
import { formatGav } from '../../utils/gav'
import { formatSizeText } from '../../utils/utils'
import { railPosition, sizeBand } from '../package/effective/sizeRail'

const COLUMNS = 'grid grid-cols-[minmax(0,1fr)_96px_120px_82px] items-center gap-x-3.5'

/** shrunken effective-size rail: same log scale and verdict colouring, minus the ticks */
function MiniSizeRail(props: { selfBytes: number, totalBytes: number }) {
  const band = () => sizeBand(props.totalBytes)
  const hasDeps = () => props.totalBytes > props.selfBytes
  return (
    <div class="relative h-[5px] overflow-hidden rounded-(--radius-bar) bg-(--track)">
      <div class="absolute inset-y-0 left-0 rounded-(--radius-bar)"
           style={{ width: `${railPosition(props.totalBytes)}%`, background: band().chevrons }}/>
      <Show when={props.selfBytes > 0}>
        <div class={['absolute inset-y-0 left-0', hasDeps() ? 'rounded-l-(--radius-bar) shadow-[1px_0_0_var(--track)]' : 'rounded-(--radius-bar)']}
             style={{ width: `${railPosition(props.selfBytes)}%`, background: band().color }}/>
      </Show>
    </div>
  )
}

function Row(props: { pkg: RecentPackage }) {
  const band = () => sizeBand(props.pkg.effectiveSize)
  const versionLabel = () => props.pkg.classifier ? `${props.pkg.version}:${props.pkg.classifier}` : props.pkg.version
  return (
    <a href={`/packages/${formatGav(props.pkg)}`}
       class={[COLUMNS, 'border-b border-(--track) px-1.5 py-2.5 text-(--ink) hover:bg-(--surface)']}>
      <div class="min-w-0 truncate font-(family-name:--font-data) text-[13.5px]">
        <span class="text-(--ink-5)">{props.pkg.groupId}</span>
        <span class="text-(--ink-mute)">:</span>
        <span class="font-medium">{props.pkg.artifactId}</span>
      </div>
      <div class="truncate font-(family-name:--font-data) text-[12px] text-(--ink-4)" title={versionLabel()}>{versionLabel()}</div>
      <MiniSizeRail selfBytes={props.pkg.packageSize} totalBytes={props.pkg.effectiveSize}/>
      <div class="text-right font-(family-name:--font-data) text-(length:--text-meta)" style={{ color: band().color }}
           title={`${band().label} — effective size incl. dependencies`}>
        {formatSizeText(props.pkg.effectiveSize)}
      </div>
    </a>
  )
}

export function RecentlyViewed() {
  return (
    <Show when={recentlyViewed().length > 0}>
      <section class="mt-[108px]">
        <div class="flex items-baseline gap-3">
          <h2 class="text-[17px] font-semibold tracking-(--tracking-tight)">Recently viewed</h2>
          <span class="text-[13px] text-(--ink-5)">on this machine</span>
        </div>
        <div class="mt-2.5 border-t border-(--hairline)">
          <For each={recentlyViewed()}>
            {pkg => <Row pkg={pkg}/>}
          </For>
        </div>
      </section>
    </Show>
  )
}
