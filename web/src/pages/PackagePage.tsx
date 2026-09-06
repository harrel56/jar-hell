import { createMemo } from 'solid-js'
import type { RouteSectionProps } from '@solidjs/router'
import { type ArtifactTree } from '../api'

export function PackagePage(props: RouteSectionProps<Promise<ArtifactTree>>) {
  const pkg = createMemo(() => props.data)
  const info = () => pkg().artifactInfo

  return (
    <main class="mx-auto max-w-(--measure-app) px-7 py-10">
      <h1 class="font-bold tracking-(--tracking-tighter) text-(length:--text-brand)">
        {info().groupId}:{info().artifactId}
      </h1>
      <p class="mt-2 font-(family-name:--font-data) text-(length:--text-sm) text-(--ink-3)">
        {info().version}
      </p>
      <p class="mt-4 text-(--ink-2)">{info().description}</p>
      <pre class="mt-8 overflow-auto rounded-(--radius-panel) border border-(--hairline) bg-(--surface-sunken) p-4 font-(family-name:--font-data) text-(length:--text-meta) text-(--ink-2)">
        {JSON.stringify(pkg(), null, 2)}
      </pre>
    </main>
  )
}
