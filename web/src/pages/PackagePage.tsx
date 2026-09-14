import {createEffect, createMemo, createSignal, Errored, latest, Loading, Show} from 'solid-js'
import {useParams} from '@solidjs/router'
import { VersionsSidebar, VersionsSidebarSkeleton } from '../components/VersionsSidebar'
import { NotFoundView, ThrownErrorView } from '../components/ErrorView'
import { PackageView } from '../components/package/PackageView'
import { PackageMainSkeleton } from '../components/package/PackageMainSkeleton'
import { AnalysisFailedView } from '../components/package/AnalysisFailedView'
import {analyzePackage, ArtifactTree, getPackage, getVersions} from '../api'
import {formatGav, Gav, parseGav} from '../utils/gav'

const isNotFound = (tree: ArtifactTree) =>
  tree.artifactInfo.unresolved === true && (tree.artifactInfo.unresolvedReason?.includes('ArtifactNotFoundException') ?? false)

const isFailed = (tree: ArtifactTree) => tree.artifactInfo.unresolved === true

export function PackagePage() {
  const params = useParams()
  const gav = createMemo(() => parseGav(params['coordinate'])!)

  const versions = createMemo(() => getVersions(gav().groupId, gav().artifactId, gav().classifier))
  const analyzed = createMemo(() => versions().some(av => av.version === gav().version && av.state !== 'NOT_ANALYZED'))
  const [pkg, setPkg] = createSignal(() => (gav(), analyzed()) ? getPackage(formatGav(gav())) : undefined)
  const [retrying, setRetrying] = createSignal(() => (gav(), false))

  const analyze = async (g: Gav) => {
    const data = await analyzePackage(g)
    if (g === latest(gav)) {
      setPkg(data)
    }
  }
  // don't wait reactively for analysis to finish, but we want to get notified when it finishes (and use it returned data)
  createEffect(
    () => analyzed() ? undefined : gav(),
    g => {
      if (g) {
        analyze(g)
      }
    }
  )

  const retry = async () => {
    const g = gav()
    setRetrying(true)
    await analyze(g)
    if (g === latest(gav)) {
      setRetrying(false)
    }
  }

  return (
    <div class="mx-auto flex w-full max-w-(--measure-app) items-start">
      <Loading fallback={<VersionsSidebarSkeleton/>}>
        <VersionsSidebar gav={gav()} versions={versions()}/>
      </Loading>

      <Errored fallback={err => <ThrownErrorView error={err()}/>}>
        <Loading on={analyzed} fallback={<PackageMainSkeleton gav={gav()} analyzing={false}/>}>
          <Loading fallback={<PackageMainSkeleton gav={gav()} analyzing={!analyzed()}/>}>
            <Show when={!retrying() && pkg()} fallback={<PackageMainSkeleton gav={gav()} analyzing={!analyzed() || retrying()}/>}>
              {pkg =>
                <Show when={!isNotFound(pkg())} fallback={<NotFoundView/>}>
                  <Show when={!isFailed(pkg())} fallback={<AnalysisFailedView info={pkg().artifactInfo} onRetry={retry}/>}>
                    <PackageView tree={pkg()}/>
                  </Show>
                </Show>}
            </Show>
          </Loading>
        </Loading>
      </Errored>
    </div>
  )
}
