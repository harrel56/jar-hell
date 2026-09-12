import { ArtifactTree } from '../../api'
import { PackageHeader } from './PackageHeader'
import { InstallSnippet } from './InstallSnippet'

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
    </main>
  )
}
