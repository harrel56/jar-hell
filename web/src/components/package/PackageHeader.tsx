import { createMemo, For, Show } from 'solid-js'
import { ArtifactInfo } from '../../api'

interface PackageHeaderProps {
  info: ArtifactInfo
}

interface ProjectLink {
  label: string
  href: string
}

const formatDate = (isoDateTime: string) => {
  const [year, month, day] = isoDateTime.slice(0, 10).split('-')
  return `${day}/${month}/${year}`
}

/** `2026-02-27T12:34:56.789` → `27/02/2026 12:34:56` */
const formatDateTime = (isoDateTime: string) => `${formatDate(isoDateTime)} ${isoDateTime.slice(11, 19)}`

function DateLabel(props: { isoDateTime: string }) {
  return (
    <span title={formatDateTime(props.isoDateTime)} class="font-(family-name:--font-data) text-(--ink-2)">
      {formatDate(props.isoDateTime)}
    </span>
  )
}

export function PackageHeader(props: PackageHeaderProps) {
  const links = createMemo<ProjectLink[]>(() => [
    { label: 'Homepage', href: props.info.url },
    { label: 'Source', href: props.info.scmUrl },
    { label: 'Issues', href: props.info.issuesUrl },
  ].filter((l): l is ProjectLink => !!l.href))

  return (
    <div class="min-w-[340px] flex-1">
      <div class="font-(family-name:--font-data) text-(length:--text-sm) text-(--ink-4)">{props.info.groupId}</div>
      <div class="mt-[5px] flex items-center gap-3">
        <h1 class="font-(family-name:--font-data) text-[36px] font-bold leading-[1.1] tracking-(--tracking-tighter) text-(--ink)">
          {props.info.artifactId}
        </h1>
        <span class="rounded-[6px] border border-(--accent-wash-border) bg-(--accent-wash) px-[9px] py-[3px] font-(family-name:--font-data) text-[14px] text-(--accent)">
          {props.info.version}
        </span>
      </div>

      <div class="mt-2.5 flex flex-wrap items-center gap-x-3.5 gap-y-1.5 text-(length:--text-meta) text-(--ink-4)">
        <Show when={props.info.created}>
          {created => (
            <span>Published <DateLabel isoDateTime={created()}/></span>
          )}
        </Show>
        <Show when={props.info.created && props.info.analyzed}>
          <span class="size-[3px] rounded-full bg-(--ink-mute)"/>
        </Show>
        <Show when={props.info.analyzed}>
          {analyzed => (
            <span>Analysed <DateLabel isoDateTime={analyzed()}/></span>
          )}
        </Show>
      </div>

      <Show when={props.info.description}>
        <p class="mt-3.5 max-w-[520px] text-(length:--text-lead) text-(--ink-2)">{props.info.description}</p>
      </Show>

      <div class="mt-[18px] flex flex-wrap items-center gap-x-[18px] gap-y-1.5">
        {/* TODO: badges dialog */}
        <button type="button"
                class="flex cursor-pointer items-center gap-[7px] whitespace-nowrap rounded-(--radius-button) border border-(--hairline-strong) px-3 py-1.5 text-(length:--text-sm) text-(--ink) hover:border-(--ink)">
          Badges
        </button>
        <For each={links()} fallback={<span class="text-(length:--text-sm) text-(--ink-5)">No project links in the package metadata</span>}>
          {link => (
            <a href={link.href} target="_blank" rel="noopener"
               class="flex items-center gap-1.5 whitespace-nowrap text-[13.5px] text-(--ink-2) hover:text-(--accent)">
              <span class="text-[11px] text-(--ink-5)">↗</span>
              <span class="border-b border-(--hairline-tick) pb-px">{link.label}</span>
            </a>
          )}
        </For>
      </div>
    </div>
  )
}
