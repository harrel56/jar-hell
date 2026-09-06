import { For } from 'solid-js'
import { useLinkState } from '@solidjs/router'
import type { Gav } from '../utils/gav'
import {ArtifactVersion} from '../api'

interface VersionsSidebarProps {
  gav: Gav
  versions: ArtifactVersion[]
}

const asideClass = 'sticky top-(--header-height) max-h-[calc(100vh-var(--header-height))] w-(--sidebar-width) shrink-0 self-start overflow-y-auto border-r border-(--hairline) px-5 pt-(--space-9) pb-10'
const rowClass = 'flex items-center gap-2 rounded-r-(--radius-nav) border-l-2 px-2.5 py-[7px] font-(family-name:--font-data) text-(length:--text-sm)'
const listClass = 'flex flex-col gap-px border-t border-(--hairline-soft) pt-2'

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
    </a>
  )
}

export function VersionsSidebar(props: VersionsSidebarProps) {
  return (
    <aside class={asideClass}>
      <RailHeader count={props.versions.length}/>
      <div class={listClass}>
        <For each={props.versions}>
          {version => <VersionLink gav={props.gav} version={version}/>}
        </For>
      </div>
    </aside>
  )
}

export function VersionsSidebarFallback() {
  return (
    <aside class={asideClass}>
      <RailHeader/>
      <div class={`${listClass} animate-pulse`}>
        <For each={Array.from({ length: 8 })}>
          {() => <div class={`${rowClass} border-transparent`}><span class="h-3 w-20 rounded-(--radius-bar) bg-(--track)"/></div>}
        </For>
      </div>
    </aside>
  )
}
