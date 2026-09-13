import { Show } from 'solid-js'
import { ArtifactTree } from '../../api'
import { PackageHeader } from './PackageHeader'
import { InstallSnippet } from './InstallSnippet'
import { EffectiveCost } from './effective/EffectiveCost'
import { MessageBlock } from '../MessageBlock'
import { InsideJar } from './jar/InsideJar'
import { DependencyExplorer } from './explorer/DependencyExplorer'

interface PackageViewProps {
  tree: ArtifactTree
}

export function PackageView(props: PackageViewProps) {
  return (
    <main class="min-w-0 flex-1 px-10 pt-(--space-10) pb-24">
      <div class="flex flex-wrap items-start gap-9">
        <PackageHeader info={props.tree.artifactInfo}/>
        <InstallSnippet gav={props.tree.artifactInfo}/>
      </div>
      <Show when={props.tree.artifactInfo.effectiveValues}>
        {effective => (
          <>
            <Show when={effective().unresolvedDependencies > 0}>
              <MessageBlock type="warning" class="mt-(--space-11)">
                {effective().unresolvedDependencies} required {effective().unresolvedDependencies === 1 ? 'dependency' : 'dependencies'} could
                not be resolved, so the effective values below are incomplete.
              </MessageBlock>
            </Show>
            <EffectiveCost info={props.tree.artifactInfo} effective={effective()}/>
          </>
        )}
      </Show>
      <Show when={props.tree.artifactInfo.jarInfo}>
        {jar => <InsideJar info={props.tree.artifactInfo} jar={jar()}/>}
      </Show>
      <Show when={props.tree.dependencies}>
        <DependencyExplorer tree={props.tree}/>
      </Show>
    </main>
  )
}
