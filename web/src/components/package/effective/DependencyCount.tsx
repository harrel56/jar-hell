import { Show } from 'solid-js'
import { JSX } from '@solidjs/web'
import { Verdict, verdict, VerdictPill } from './VerdictPill'

export const requiredVerdict = (count: number): Verdict => {
  if (count === 0) return verdict('Zero-dep', 'good')
  if (count <= 2) return verdict('Few', 'good')
  if (count <= 8) return verdict('Several', 'warn')
  return verdict('Many', 'critical')
}

interface DependencyCountProps {
  count: number
  title: string
  verdict?: Verdict | undefined
  children: JSX.Element
}

export function DependencyCount(props: DependencyCountProps) {
  return (
    <div class="bg-(--ground) px-6 pt-[22px] pb-5">
      <div class="flex flex-wrap items-end gap-4">
        <div class="flex items-baseline gap-[9px] whitespace-nowrap font-(family-name:--font-data)">
          <span class="text-(length:--text-metric-lg) font-medium leading-(--leading-metric) tracking-[-0.04em]">{props.count}</span>
          <span class="text-[18px] text-(--ink-3)">{props.count === 1 ? 'artifact' : 'artifacts'}</span>
        </div>
        <div class="flex items-center gap-2.5 pb-1">
          <span class="text-[15px] font-semibold">{props.title}</span>
          <Show when={props.verdict}>{verdict => <VerdictPill verdict={verdict()}/>}</Show>
        </div>
      </div>
      <div class="mt-4 text-(length:--text-meta) text-(--ink-3)">{props.children}</div>
    </div>
  )
}
