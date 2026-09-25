import {createMemo, createSignal, For, Repeat} from 'solid-js'
import {useLinkState} from '@solidjs/router'
import {Gav} from '../utils/gav'
import {ArtifactVersion} from '../api'
import {calculateVersionNodes, versionHref} from '../utils/versions'
import { VersionStatePill } from './VersionStatePill'
import { Icon } from '../icons'

interface VersionsSidebarProps {
  gav: Gav
  versions: readonly ArtifactVersion[]
}

// below md the rail is replaced by VersionsPicker, rendered next to the package name
const asideClass = 'max-md:hidden sticky top-(--header-height) max-h-[calc(100vh-var(--header-height))] w-(--sidebar-width) shrink-0 self-start overflow-y-auto border-r border-(--hairline) px-5 pt-(--space-9) pb-10'
const rowClass = 'flex items-center gap-2 rounded-r-(--radius-nav) border-l-2 px-2.5 py-[7px] font-(family-name:--font-data) text-(length:--text-sm)'
const groupClass = 'border-t border-(--hairline-soft)'
function RailHeader(props: { count?: number }) {
  return (
    <div class="flex items-baseline justify-between px-1.5 pb-3">
      <span class="font-semibold uppercase tracking-(--tracking-caps) text-(length:--text-meta) text-(--ink-4)">
        Versions
      </span>
      <span class="font-(family-name:--font-data) text-(length:--text-label) text-(--ink-5)">
        {props.count}
      </span>
    </div>
  )
}

interface VersionLinkProps {
  gav: Gav
  version: ArtifactVersion
  selected: boolean
  onSelect: (version: string) => void
}

function VersionLink(props: VersionLinkProps) {
  const href = createMemo(() => versionHref(props.gav, props.version.version))

  const link = useLinkState(() => href())

  return (
    <a
      href={href()}
      onClick={() => props.onSelect(props.version.version)}
      aria-current={props.selected ? 'true' : undefined}
      data-pending={link.pending() || undefined}
      class={[
        rowClass,
        {
          'border-(--accent) bg-(--surface-sunken) text-(--accent)': props.selected,
          'border-transparent text-(--ink-2) hover:bg-(--track)': !props.selected,
        },
      ]}
    >
      {props.version.version}
      <VersionStatePill state={props.version.state} class="ml-auto"/>
    </a>
  )
}

interface VersionGroupProps {
  gav: Gav
  label: string
  versions: ArtifactVersion[]
  active: boolean
  opened: boolean
  selectedVersion: string | undefined
  onToggle: (group: string) => void
  onSelect: (version: string) => void
}

/**
 * Waiting for firefox to finally implement some basic css https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Properties/interpolate-size
 * Meanwhile we must simulate details-summary component to enable animations
 */
function VersionGroup(props: VersionGroupProps) {
  return (
    <section class={groupClass}>
      <button type="button"
              aria-expanded={props.opened ? 'true' : 'false'}
              class="flex w-full cursor-pointer items-center gap-2.5 rounded-(--radius-nav) px-1.5 py-2.5 hover:bg-(--surface-sunken) text-(--ink)"
              onClick={() => props.onToggle(props.label)}>
        <span class={[{'text-(--accent)': props.active}, 'font-(family-name:--font-data) font-medium text-(length:--text-sm)']}>
          {`${props.label}.x`}
        </span>
        <span class="text-(length:--text-label) text-(--ink-5) leading-0">
          {props.versions.length} {props.versions.length === 1 ? 'item' : 'items'}
        </span>
        <span class={['ml-auto text-(--ink-5) transition-transform', {'rotate-180': props.opened}]}>
          <Icon.ChevronDown class="size-2.5"/>
        </span>
      </button>
      <div class={['grid transition-[grid-template-rows] duration-(--duration-rail) ease-(--ease-rail)',
        {'grid-rows-[0fr]': !props.opened, 'grid-rows-[1fr]': props.opened}]} inert={!props.opened}>
        <div class="overflow-hidden flex flex-col gap-px mb-2 pl-1.5">
          <For each={props.versions}>
            {version => (
              <VersionLink
                gav={props.gav}
                version={version}
                selected={version.version === props.selectedVersion}
                onSelect={props.onSelect}
              />
            )}
          </For>
        </div>
      </div>
    </section>
  )
}

export function VersionsSidebar(props: VersionsSidebarProps) {
  // as a workaround for "broken" useLinkState - it does not change to pending on click immediately
  const [selectedVersion, setSelectedVersion] = createSignal(() => props.gav.version)

  const nodes = createMemo(() => Array.from(calculateVersionNodes(props.versions.toReversed()).entries()))
  const activeGroup = createMemo(() =>
    nodes().find(e => e[1].some(av => av.version === selectedVersion()))?.[0]
  )
  const [openedGroup, setOpenedGroup] = createSignal(activeGroup)

  return (
    <aside class={asideClass}>
      <RailHeader count={props.versions.length}/>
      <For each={nodes()}>
        {([label, versions]) => (
          <VersionGroup
            gav={props.gav}
            label={label}
            versions={versions}
            active={label === activeGroup()}
            opened={label === openedGroup()}
            selectedVersion={selectedVersion()}
            onToggle={(group: string) => setOpenedGroup(prev => group === prev ? undefined : group)}
            onSelect={setSelectedVersion}
          />
        )}
      </For>
    </aside>
  )
}

export function VersionsSidebarSkeleton() {
  return (
    <aside class={asideClass}>
      <RailHeader/>
      <div class={`${groupClass} flex animate-pulse flex-col gap-px pt-2`}>
        <Repeat count={8}>
          {() => <div class={`${rowClass} border-transparent`}><span class="h-3 w-16 my-1 rounded-(--radius-bar) bg-(--track)"/></div>}
        </Repeat>
      </div>
    </aside>
  )
}
