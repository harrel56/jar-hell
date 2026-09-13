import {createMemo, Errored, isPending, latest, Loading, Show, untrack} from 'solid-js'
import { useParams } from '@solidjs/router'
import { VersionsSidebar, VersionsSidebarSkeleton } from '../components/VersionsSidebar'
import { NotFoundView, ThrownErrorView } from '../components/ErrorView'
import { PackageView } from '../components/package/PackageView'
import { PackageMainSkeleton } from '../components/package/PackageMainSkeleton'
import {analyzePackage, ArtifactTree, getPackage, getVersions} from '../api'
import {formatGav, Gav, parseGav} from '../utils/gav'

const isNotFound = (tree: ArtifactTree) =>
  tree.artifactInfo.unresolved === true && (tree.artifactInfo.unresolvedReason?.includes('ArtifactNotFoundException') ?? false)

export function PackagePage() {
  const params = useParams()
  const gav = createMemo(() => parseGav(params['coordinate'])!)

  const versions = createMemo(() => getVersions(gav().groupId, gav().artifactId, gav().classifier))
  const isAnalyzed = (gav: Gav) => untrack(versions).some(av => av.version === gav.version && av.analyzed)
  const pkg = createMemo(() => isAnalyzed(gav()) ? getPackage(formatGav(gav())) : analyzePackage(gav()))

  return (
    <div class="mx-auto flex w-full max-w-(--measure-app) items-start">
      <Loading fallback={<VersionsSidebarSkeleton/>}>
        <VersionsSidebar gav={gav()} versions={versions()}/>
      </Loading>

      <Errored fallback={err => <ThrownErrorView error={err()}/>}>
        <Loading fallback={<PackageMainSkeleton gav={latest(gav)} analyzing={!isPending(versions) && !isAnalyzed(latest(gav))}/>}>
          <Show when={!isPending(pkg) || isAnalyzed(latest(gav))} fallback={<PackageMainSkeleton gav={latest(gav)} analyzing={!isAnalyzed(latest(gav))}/>}>
            <Show when={!isNotFound(pkg())} fallback={<NotFoundView/>}>
              <PackageView tree={pkg()}/>
            </Show>
          </Show>
        </Loading>
      </Errored>
    </div>
  )
}
