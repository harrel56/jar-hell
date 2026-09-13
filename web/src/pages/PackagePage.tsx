import {createEffect, createMemo, createSignal, Errored, latest, Loading, Show} from 'solid-js'
import {useParams} from '@solidjs/router'
import { VersionsSidebar, VersionsSidebarSkeleton } from '../components/VersionsSidebar'
import { NotFoundView, ThrownErrorView } from '../components/ErrorView'
import { PackageView } from '../components/package/PackageView'
import { PackageMainSkeleton } from '../components/package/PackageMainSkeleton'
import {analyzePackage, ArtifactTree, getPackage, getVersions} from '../api'
import {formatGav, parseGav} from '../utils/gav'

const isNotFound = (tree: ArtifactTree) =>
  tree.artifactInfo.unresolved === true && (tree.artifactInfo.unresolvedReason?.includes('ArtifactNotFoundException') ?? false)

export function PackagePage() {
  const params = useParams()
  const gav = createMemo(() => parseGav(params['coordinate'])!)

  const versions = createMemo(() => getVersions(gav().groupId, gav().artifactId, gav().classifier))
  const analyzed = createMemo(() => versions().some(av => av.version === gav().version && av.analyzed))
  const [pkg, setPkg] = createSignal(() => (gav(), analyzed()) ? getPackage(formatGav(gav())) : undefined)

  // don't wait reactively for analysis to finish, but we want to get notified when it finishes (and use it returnde data)
  createEffect(
    () => analyzed() ? undefined : gav(),
    g => {
      if (g) {
        analyzePackage(g).then(data => {
          if (g === latest(gav)) {
            setPkg(data)
          }
        })
      }
    }
  )

  return (
    <div class="mx-auto flex w-full max-w-(--measure-app) items-start">
      <Loading fallback={<VersionsSidebarSkeleton/>}>
        <VersionsSidebar gav={gav()} versions={versions()}/>
      </Loading>

      <Errored fallback={err => <ThrownErrorView error={err()}/>}>
        <Loading on={analyzed} fallback={<PackageMainSkeleton gav={gav()} analyzing={false}/>}>
          <Loading fallback={<PackageMainSkeleton gav={gav()} analyzing={!analyzed()}/>}>
            <Show when={pkg()} fallback={<PackageMainSkeleton gav={gav()} analyzing={!analyzed()}/>}>
              {pkg =>
                <Show when={!isNotFound(pkg())} fallback={<NotFoundView/>}>
                  <PackageView tree={pkg()}/>
                </Show>}
            </Show>
          </Loading>
        </Loading>
      </Errored>
    </div>
  )
}
