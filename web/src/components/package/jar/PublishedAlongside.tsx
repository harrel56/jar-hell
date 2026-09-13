import { createMemo, For, Show } from 'solid-js'
import { ArtifactInfo } from '../../../api'
import { StatusDot } from './StatusDot'

interface Flag {
  label: string
  present: boolean
  detail: string
  link?: { label: string, href: string }
}

const CHECKSUMS = ['md5', 'sha1', 'sha256', 'sha512']

const flags = (info: ArtifactInfo): Flag[] => {
  const classifiers = info.classifiers ?? []
  const extensions = info.extensions ?? []
  const checksums = CHECKSUMS.filter(sum => extensions.includes(sum))
  return [
    {
      label: 'Sources',
      present: classifiers.includes('sources'),
      detail: `${info.artifactId}-${info.version}-sources.jar`,
    },
    {
      label: 'Javadoc',
      present: classifiers.includes('javadoc'),
      detail: `${info.artifactId}-${info.version}-javadoc.jar`,
      link: { label: 'Browse ↗', href: `https://javadoc.io/doc/${info.groupId}/${info.artifactId}/${info.version}` },
    },
    {
      label: 'Gradle module metadata',
      present: extensions.includes('module'),
      detail: '.module',
    },
    {
      label: 'Signed',
      present: extensions.includes(`asc`),
      detail: 'PGP .asc',
    },
    {
      label: 'Checksums',
      present: checksums.length > 0,
      detail: checksums.join(' · '),
    },
  ]
}

export function PublishedAlongside(props: { info: ArtifactInfo }) {
  const rows = createMemo(() => flags(props.info))

  return (
    <div class="border-t border-(--hairline) bg-(--ground) px-6 pt-3.5 pb-4">
      <div class="text-[12px] font-semibold uppercase tracking-(--tracking-caps) text-(--ink-4)">Published alongside</div>
      <div class="mt-[11px] flex flex-wrap gap-x-2.5 gap-y-2">
        <For each={rows()}>
          {flag => (
            <div class={['flex items-center gap-2 rounded-[9px] border py-[7px] pr-3 pl-2.5',
              flag.present ? 'border-(--good-wash-border) bg-(--good-wash)' : 'border-(--hairline) bg-(--surface)']}>
              <StatusDot type={flag.present ? 'check' : 'x'}/>
              <span class={['text-(length:--text-sm)', flag.present ? 'text-(--ink)' : 'text-(--ink-4)']}>{flag.label}</span>
              <Show when={flag.present && flag.detail}>
                <span class="font-(family-name:--font-data) text-(length:--text-label) text-(--ink-5)">{flag.detail}</span>
              </Show>
              <Show when={flag.present && flag.link}>
                {link => <a href={link().href} target="_blank" rel="noopener" class="text-(length:--text-meta) font-medium hover:text-(--accent)">{link().label}</a>}
              </Show>
            </div>
          )}
        </For>
      </div>
    </div>
  )
}
