import { ArtifactTree } from '../../../api'
import { MessageBlock } from '../../MessageBlock'
import { DependencyNode, EXPLORER_COLUMNS } from './DependencyNode'

export function DependencyExplorer(props: { tree: ArtifactTree }) {
  return (
    <section class="mt-(--space-11)">
      <div class="flex items-center gap-3.5">
        <span class="h-[18px] w-[3px] shrink-0 rounded-[2px] bg-(--accent)"/>
        <h2 class="text-[20px] font-semibold tracking-(--tracking-tight)">Dependency explorer</h2>
      </div>

      <MessageBlock type="warning" class="mt-3.5">
        The tree does not account for excluded packages or version conflicts, so resolved builds may differ.
      </MessageBlock>

      <div class="mt-3.5 overflow-hidden rounded-(--radius-card) border border-(--hairline)">
        <div class={[EXPLORER_COLUMNS, 'border-b border-(--hairline) bg-(--surface) px-[18px] py-[11px] text-(length:--text-label) font-semibold uppercase tracking-[0.07em] text-(--ink-4)']}>
          <div>Package</div>
          <div class="text-right">Size</div>
          <div class="text-right" title="Minimum Java version the jar's class files require">Java</div>
          <div class="text-right">License</div>
        </div>
        <DependencyNode tree={props.tree} depth={0} optional={false} initiallyOpened/>
        <div class="flex px-[18px] py-3 bg-(--surface) text-(length:--text-meta) text-(--ink-4)">
          <span class="ml-auto">Expand a node to load its dependencies · optional branches are dimmed</span>
        </div>
      </div>
    </section>
  )
}
