import {createMemo, Errored, isPending, Loading, Show} from 'solid-js'
import { useParams } from '@solidjs/router'
import { VersionsSidebar, VersionsSidebarSkeleton } from '../components/VersionsSidebar'
import { NotFoundView, ThrownErrorView } from '../components/ErrorView'
import {analyzePackage, ArtifactTree, getPackage, getVersions} from '../api'
import { Gav, parseGav } from '../utils/gav'

const isNotFound = (tree: ArtifactTree) =>
  tree.artifactInfo.unresolved === true && (tree.artifactInfo.unresolvedReason?.includes('ArtifactNotFoundException') ?? false)

export function PackagePage() {
  const params = useParams()
  const coordinate = () => params['coordinate']!
  const gav = createMemo(() => parseGav(coordinate())!)
  const versionsArgs = createMemo(() => [gav().groupId, gav().artifactId, gav().classifier] as const)

  const versions = createMemo(() => getVersions(...versionsArgs()))
  const analyzed = createMemo(() => versions().some(av => av.version === gav().version && av.analyzed))
  const pkg = createMemo(() => analyzed() ? getPackage(coordinate()) : analyzePackage(gav()))

  return (
    <div class="mx-auto flex max-w-(--measure-app) items-start">
      <Loading fallback={<VersionsSidebarSkeleton/>}>
        <VersionsSidebar gav={gav()} versions={versions()}/>
      </Loading>

      <Errored fallback={err => <ThrownErrorView error={err()} requested={coordinate()}/>}>
        <Loading fallback={<PackageMainSkeleton gav={gav()}/>}>
          <Show when={analyzed() || !isPending(pkg)} fallback={<PackageMainSkeleton gav={gav()}/>}>
            <Show when={!isNotFound(pkg())} fallback={<NotFoundView/>}>
              <main class="min-w-0 flex-1 px-10 pt-(--space-10) pb-24">
                <PackageHeading gav={gav()}/>
                <p class="mt-4 text-(--ink-2)">{pkg().artifactInfo.description}</p>
                <pre class="mt-8 overflow-auto rounded-(--radius-panel) border border-(--hairline) bg-(--surface-sunken) p-4 font-(family-name:--font-data) text-(length:--text-meta) text-(--ink-2)">
                  {JSON.stringify(pkg(), null, 2)}
                </pre>
              </main>
            </Show>
          </Show>
        </Loading>
      </Errored>
    </div>
  )
}

function PackageHeading(props: { gav: Gav }) {
  return (
    <>
      <h1 class="font-bold tracking-tighter text-(length:--text-brand)">
        {props.gav.groupId}:{props.gav.artifactId}
      </h1>
      <p class="mt-2 font-(family-name:--font-data) text-(length:--text-sm) text-(--ink-3)">
        {props.gav.version}
      </p>
    </>
  )
}

function PackageMainSkeleton(props: { gav: Gav }) {
  return (
    <main class="min-w-0 flex-1 px-10 pt-(--space-10) pb-24">
      <PackageHeading gav={props.gav}/>
      <div class="mt-8 h-64 animate-pulse rounded-(--radius-panel) bg-(--track)"/>
    </main>
  )
}
