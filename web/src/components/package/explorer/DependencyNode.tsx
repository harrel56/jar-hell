import { createMemo, createSignal, For, Loading, Show } from 'solid-js'
import { ArtifactTree, DependencyInfo, getPackage } from '../../../api'
import { formatGav } from '../../../utils/gav'
import { formatBytecodeVersion, formatLicenseType, formatSizeText } from '../../../utils/utils'
import { Icon } from '../../../icons'

export const EXPLORER_COLUMNS = 'grid grid-cols-[minmax(0,1fr)_90px_64px_104px] gap-x-5'

interface DependencyNodeProps {
  tree: ArtifactTree
  depth: number
  optional: boolean
  initiallyOpened?: boolean | undefined
}

export function DependencyNode(props: DependencyNodeProps) {
  const [opened, setOpened] = createSignal(props.initiallyOpened ?? false)
  const coordinate = () => formatGav(props.tree.artifactInfo)
  const isLeaf = () => {
    const effective = props.tree.artifactInfo.effectiveValues
    return effective !== undefined
      && effective.requiredDependencies + effective.optionalDependencies + effective.unresolvedDependencies === 0
  }

  const dependencies = createMemo<DependencyInfo[] | undefined>(() => {
    if (props.tree.dependencies) return props.tree.dependencies
    if (!opened()) return undefined
    return getPackage(coordinate()).then(tree => tree.dependencies ?? [])
  })

  const licenseType = () => props.tree.artifactInfo.licenseTypes?.[0] ?? 'NO_LICENSE'
  const license = () => formatLicenseType(licenseType())[0]
  const licenseTitle = () => licenseType() === 'UNKNOWN' ? props.tree.artifactInfo.licenses?.[0]?.name : license()

  const bytecode = () => {
    const version = props.tree.artifactInfo.jarInfo?.bytecodeVersion
    return version ? formatBytecodeVersion(version) : '–'
  }

  return (
    <>
      <div class={[EXPLORER_COLUMNS, 'items-center border-b border-(--track) px-[18px] hover:bg-(--surface)', { 'bg-(--surface-row)': props.depth === 0 }]}>
        <div class="flex min-w-0 items-center gap-[9px] py-2.5" style={{ 'padding-left': `${props.depth * 22}px` }}>
          <Show when={!isLeaf()} fallback={<Icon.Circle class="size-[17px] shrink-0 text-(--hairline-tick)"/>}>
            <button type="button" aria-expanded={opened() ? 'true' : 'false'} aria-label={opened() ? 'Collapse' : 'Expand'}
                    class="flex shrink-0 cursor-pointer text-(--ink-2) hover:text-(--accent)"
                    onClick={() => setOpened(o => !o)}>
              <Show when={opened()} fallback={<Icon.CirclePlus class="size-[17px]"/>}>
                <Icon.CircleMinus class="size-[17px]"/>
              </Show>
            </button>
          </Show>
          <a href={`/packages/${coordinate()}`} title={coordinate()}
             class={['min-w-0 truncate font-(family-name:--font-data) text-[13.5px] hover:text-(--accent) hover:underline', props.optional ? 'text-(--ink-2)' : 'text-(--ink)']}>
            {props.tree.artifactInfo.groupId}:{props.tree.artifactInfo.artifactId}
          </a>
          <span class="shrink-0 whitespace-nowrap font-(family-name:--font-data) text-(length:--text-label) text-(--ink-5)">
            {props.tree.artifactInfo.version}
            <Show when={props.tree.artifactInfo.classifier}>{classifier => <>:{classifier()}</>}</Show>
          </span>
          <Show when={props.optional}>
            <span class="shrink-0 rounded-(--radius-pill) border border-(--hairline-strong) px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-[0.05em] text-(--ink-4)">Optional</span>
          </Show>
        </div>
        <div class={['text-right font-(family-name:--font-data) text-(length:--text-meta)', props.optional ? 'text-(--ink-5)' : 'text-(--ink-3)']}>
          {formatSizeText(props.tree.artifactInfo.packageSize ?? 0)}
        </div>
        <div class={['text-right font-(family-name:--font-data) text-(length:--text-meta)', props.optional ? 'text-(--ink-5)' : 'text-(--ink-3)']}>
          {bytecode()}
        </div>
        <div class={['truncate text-right text-[12px]', props.optional ? 'text-(--ink-5)' : 'text-(--ink-3)']} title={licenseTitle()}>{license()}</div>
      </div>
      <Show when={opened()}>
        <Loading fallback={<LoadingRow depth={props.depth + 1}/>}>
          <For each={dependencies()}>
            {dep => <DependencyNode tree={dep.artifact} depth={props.depth + 1} optional={props.optional || dep.optional}/>}
          </For>
        </Loading>
      </Show>
    </>
  )
}

function LoadingRow(props: { depth: number }) {
  return (
    <div class="border-b border-(--track) px-[18px] py-2.5" style={{ 'padding-left': `${18 + props.depth * 22}px` }}>
      <div class="h-3.5 w-48 animate-pulse rounded-(--radius-bar) bg-(--track)"/>
    </div>
  )
}
