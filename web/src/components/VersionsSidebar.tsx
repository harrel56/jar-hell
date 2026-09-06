import {createMemo, For, Repeat, Show} from 'solid-js'
import {useLinkState} from '@solidjs/router'
import {Gav} from '../utils/gav'
import {ArtifactVersion} from '../api'
import { Icon } from '../icons'

interface VersionsSidebarProps {
  gav: Gav
  versions: ArtifactVersion[]
}

const asideClass = 'sticky top-(--header-height) max-h-[calc(100vh-var(--header-height))] w-(--sidebar-width) shrink-0 self-start overflow-y-auto border-r border-(--hairline) px-5 pt-(--space-9) pb-10'
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

function VersionLink(props: { gav: Gav, version: ArtifactVersion}) {
  const gav = props.gav
  const classifierPart = gav.classifier ? `:${gav.classifier}` : ''
  const href = `/packages/${gav.groupId}:${gav.artifactId}:${props.version.version}${classifierPart}`
  const link = useLinkState(() => href)
  const selected = () => link.current() || link.pending()

  return (
    <a
      href={href}
      data-pending={link.pending() || undefined}
      class={[
        rowClass,
        {
          'border-(--accent) bg-(--surface-sunken) text-(--accent)': selected(),
          'border-transparent text-(--ink-2) hover:bg-(--track)': !selected(),
        },
      ]}
    >
      {props.version.version}
      <Show when={props.version.analyzed}>
        <span class="ml-auto rounded-(--radius-pill) border border-(--good-wash-border) bg-(--good-wash) px-1.5 py-0.5 font-semibold uppercase tracking-[0.04em] text-(length:--text-micro) text-(--good-wash-ink)">
          Analyzed
        </span>
      </Show>
    </a>
  )
}

interface VersionGroupProps {
  gav: Gav
  label: string
  versions: ArtifactVersion[]
  open: boolean
}

function VersionGroup(props: VersionGroupProps) {
  return (
    <details
      class={`${groupClass} group`}
      name="versions"
      open={props.open}
    >
      <summary class="flex cursor-pointer list-none items-center gap-2.5 rounded-(--radius-nav) px-1.5 py-2.5 hover:bg-(--surface-sunken) text-(--ink) [&::-webkit-details-marker]:hidden">
        <span class={[{'text-(--accent)': props.open}, 'font-(family-name:--font-data) font-medium text-(length:--text-sm)']}>
          {props.label}
        </span>
        <span class="text-(length:--text-label) text-(--ink-5)">
          {props.versions.length} {props.versions.length === 1 ? 'item' : 'items'}
        </span>
        <span class="ml-auto text-(--ink-5) transition-transform group-open:rotate-180">
          <Icon.ChevronDown class="size-2.5"/>
        </span>
      </summary>
      <div class="flex flex-col gap-px pb-2 pl-1.5">
        <For each={props.versions}>
          {version => <VersionLink gav={props.gav} version={version}/>}
        </For>
      </div>
    </details>
  )
}

export function VersionsSidebar(props: VersionsSidebarProps) {
  const nodes = createMemo(() => Array.from(calculateVersionNodes(props.versions.toReversed()).entries()))
  const activeGroup = createMemo(() =>
    nodes().find(e => e[1].map(av => av.version).includes(props.gav.version!))?.[0]
  )

  return (
    <aside class={asideClass}>
      <RailHeader count={props.versions.length}/>
      <For each={nodes()}>
        {([label, versions]) => (
          <VersionGroup
            gav={props.gav}
            label={`${label}.x`}
            versions={versions}
            open={label === activeGroup()}
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

/**
 * Group by minor version if:
 * - there is only 1 major
 * - in scope of 1 major there is a minor that got >= 10 patch versions
 * otherwise group by major
 * */
const calculateVersionNodes = (versions: ArtifactVersion[]) => {
  const byMajor = new Map<string, Map<string, ArtifactVersion[]>>()
  versions.forEach(av => {
    const [major, minor] = av.version.split('.', 2)
    const byMinor = byMajor.get(major!) ?? new Map<string, ArtifactVersion[]>()
    const patches = byMinor.get(minor!) ?? []
    patches.push(av)
    byMinor.set(minor!, patches)
    byMajor.set(major!, byMinor)
  })

  const nodes = new Map<string, ArtifactVersion[]>()
  Array.from(byMajor.entries()).forEach(([major, byMinor]) => {
    const expandMinor = byMajor.size === 1 || Array.from(byMinor.values()).some(patches => patches.length >= 10)
    if (expandMinor) {
      Array.from(byMinor.entries()).forEach(([minor, patches]) => {
        nodes.set(`${major}.${minor}`, patches)
      })
    } else {
      const newVersions: ArtifactVersion[] = []
      Array.from(byMinor.values()).forEach(patches => newVersions.push(...patches))
      nodes.set(major, newVersions)
    }
  })
  return nodes
}