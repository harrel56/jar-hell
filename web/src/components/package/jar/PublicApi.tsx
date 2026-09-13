import { createMemo, For } from 'solid-js'
import { JarInfo } from '../../../api'

// Backend `ClassType` values in display order
const CLASS_KINDS = [
  { type: 'CLASS', label: 'Class' },
  { type: 'INTERFACE', label: 'Interface' },
  { type: 'ABSTRACT_CLASS', label: 'Abstract class' },
  { type: 'RECORD', label: 'Record' },
  { type: 'ENUM', label: 'Enum' },
  { type: 'ANNOTATION', label: 'Annotation' },
]

const PUBLIC_COLOR = 'var(--ink-2)'

export function PublicApi(props: { jar: JarInfo }) {
  const publicTypes = createMemo(() => Object.values(props.jar.publicClasses).reduce((sum, n) => sum + n, 0))
  const typeTotal = () => publicTypes() + props.jar.nonPublicClasses
  const publicShare = () => typeTotal() === 0 ? 0 : publicTypes() / typeTotal()
  const kinds = () => CLASS_KINDS.map(kind => ({ label: kind.label, count: props.jar.publicClasses[kind.type] ?? 0 }))

  return (
    <div class="bg-(--ground) px-6 pt-6 pb-5">
      <div class="flex items-baseline gap-[9px] whitespace-nowrap">
        <span class="font-(family-name:--font-data) text-(length:--text-metric-lg) font-medium leading-(--leading-metric) tracking-[-0.04em]"
              style={{ color: PUBLIC_COLOR }}>
          {Math.round(publicShare() * 100)}%
        </span>
        <span class="text-[15px] font-semibold text-(--ink-2)">of classes are public</span>
      </div>

      <div class="mt-5 flex h-3 gap-0.5 overflow-hidden rounded-(--radius-bar) bg-(--track)">
        <div style={{ width: `${(publicShare() * 100).toFixed(1)}%`, background: PUBLIC_COLOR }}/>
        <div class="flex-1 bg-(--ink-mute)"/>
      </div>
      <div class="mt-3 flex flex-wrap gap-x-4 gap-y-1.5 text-(length:--text-meta) text-(--ink-3)">
        <span class="flex items-center gap-[7px]">
          <span class="size-2.5 shrink-0 rounded-[3px]" style={{ background: PUBLIC_COLOR }}/>
          {publicTypes()} public types
        </span>
        <span class="flex items-center gap-[7px]">
          <span class="size-2.5 shrink-0 rounded-[3px] bg-(--ink-mute)"/>
          {props.jar.nonPublicClasses} package-private or internal
        </span>
      </div>
      <div class="mt-2.5 text-(length:--text-meta) text-(--ink-4)">
        {Math.round(publicShare() * 100)}% of {typeTotal()} types are reachable from outside the jar.
      </div>

      <div class="mt-[18px] text-(length:--text-label) font-semibold uppercase tracking-[0.06em] text-(--ink-5)">Public types by kind</div>
      <div class="mt-2.5 grid grid-cols-[repeat(auto-fill,minmax(120px,1fr))] gap-2">
        <For each={kinds()}>
          {kind => (
            <div class="rounded-[9px] border border-(--hairline) bg-(--surface-row) px-3 py-2.5">
              <div class="font-(family-name:--font-data) text-[20px] font-medium tracking-(--tracking-tighter)" style={{ color: PUBLIC_COLOR }}>{kind.count}</div>
              <div class="mt-0.5 text-[12px] text-(--ink-3)">{kind.label}</div>
            </div>
          )}
        </For>
      </div>
    </div>
  )
}
