import { createMemo, Loading } from 'solid-js'
import { useParams } from '@solidjs/router'
import { VersionsSidebar, VersionsSidebarFallback } from '../components/VersionsSidebar'
import { getPackage, getVersions } from '../api'
import { parseGav } from '../utils/gav'

export function PackagePage() {
  const params = useParams()
  const coordinate = () => params['coordinate']!
  const gav = createMemo(() => parseGav(coordinate())!)
  const versions = createMemo(() => getVersions(gav().groupId, gav().artifactId, gav().classifier))
  const pkg = createMemo(() => getPackage(coordinate()))

  return (
    <div class="mx-auto flex max-w-(--measure-app) items-start">
      <Loading fallback={<VersionsSidebarFallback/>}>
        <VersionsSidebar gav={gav()} versions={versions()}/>
      </Loading>

      <main class="min-w-0 flex-1 px-10 pt-(--space-10) pb-24">
        <h1 class="font-bold tracking-tighter text-(length:--text-brand)">
          {gav().groupId}:{gav().artifactId}
        </h1>
        <p class="mt-2 font-(family-name:--font-data) text-(length:--text-sm) text-(--ink-3)">
          {gav().version}
        </p>

        <Loading fallback={<div class="mt-8 h-64 animate-pulse rounded-(--radius-panel) bg-(--track)"/>}>
          <p class="mt-4 text-(--ink-2)">{pkg().artifactInfo.description}</p>
          <pre class="mt-8 overflow-auto rounded-(--radius-panel) border border-(--hairline) bg-(--surface-sunken) p-4 font-(family-name:--font-data) text-(length:--text-meta) text-(--ink-2)">
            {JSON.stringify(pkg(), null, 2)}
          </pre>
        </Loading>
      </main>
    </div>
  )
}
