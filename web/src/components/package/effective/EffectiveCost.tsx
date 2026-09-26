import { Show } from 'solid-js'
import { ArtifactInfo, EffectiveValues } from '../../../api'
import { EffectiveSize } from './EffectiveSize'
import { DependencyCount, requiredVerdict } from './DependencyCount'
import { BytecodeVersionCell } from './BytecodeVersionCell'
import { EffectiveLicense } from './EffectiveLicense'

interface EffectiveCostProps {
  info: ArtifactInfo
  effective: EffectiveValues
}

export function EffectiveCost(props: EffectiveCostProps) {
  return (
    <section class="mt-(--space-11) overflow-hidden rounded-(--radius-section) border border-(--hairline)">
      <div class="flex flex-wrap items-center gap-x-3 gap-y-2.5 border-b border-(--hairline) bg-(--surface) px-[22px] py-3">
        <span class="h-[13px] w-[3px] shrink-0 rounded-[2px] bg-(--accent)"/>
        <span class="whitespace-nowrap text-[12px] font-semibold uppercase tracking-(--tracking-caps) text-(--ink-4)">Effective cost</span>
        <span class="text-(length:--text-meta) text-(--ink-5)">calculated from all required dependencies</span>
      </div>
      <div class="grid gap-px bg-(--hairline)">
        <EffectiveSize selfBytes={props.info.packageSize ?? 0} totalBytes={props.effective.size}/>
        <div class="grid grid-cols-[repeat(auto-fit,minmax(300px,1fr))] gap-px">
          <DependencyCount count={props.effective.requiredDependencies} title="Required dependencies"
                           verdict={requiredVerdict(props.effective.requiredDependencies)}>
            <Show when={props.effective.unresolvedDependencies > 0} fallback="Pulled in transitively by every consumer.">
              <span class="text-(--bad)">{props.effective.unresolvedDependencies} could not be resolved</span>
            </Show>
          </DependencyCount>
          <DependencyCount count={props.effective.optionalDependencies} title="Optional dependencies">
            <p>Not included in other effective values calculations.</p>
          </DependencyCount>
        </div>
        <div class="grid grid-cols-[repeat(auto-fit,minmax(300px,1fr))] gap-px">
          <BytecodeVersionCell bytecodeVersion={props.effective.bytecodeVersion}/>
          <EffectiveLicense effective={props.effective}/>
        </div>
      </div>
    </section>
  )
}
