import { createMemo, For, Show } from 'solid-js'
import { JarInfo } from '../../../api'

interface ContentRow {
  label: string
  color: string
  entries: number
  bytes: number
  /** share of the uncompressed size, 0-100 */
  sizePct: number
}

const CONTENT_LABELS: Record<string, string> = {
  RESOURCE: 'Resources',
  SYNTHETIC: 'Synthetic classes',
  UNKNOWN: 'Unknown source',
  INVALID: 'Unreadable classes',
  JRUBY: 'JRuby',
  JYTHON: 'Jython',
  X10: 'X10',
}

const LANGUAGE_COLORS = ['JAVA', 'KOTLIN', 'SCALA', 'GROOVY', 'CLOJURE', 'RESOURCE']

const contentLabel = (type: string) =>
  CONTENT_LABELS[type] ?? type.charAt(0) + type.slice(1).toLowerCase()

const contentColor = (type: string) =>
  LANGUAGE_COLORS.includes(type) ? `var(--lang-${type === 'RESOURCE' ? 'resources' : type.toLowerCase()})` : 'var(--ink-5)'

const COLUMNS = 'grid grid-cols-[minmax(0,1fr)_72px_82px] gap-x-3'

export function JarContents(props: { jar: JarInfo }) {
  // Largest first, so the first row is the headline language
  const rows = createMemo<ContentRow[]>(() => {
    const entries = Object.entries(props.jar.contents)
    const bytesTotal = entries.reduce((sum, [, c]) => sum + c.size, 0)
    return entries
      .map(([type, c]) => ({
        label: contentLabel(type),
        color: contentColor(type),
        entries: c.count,
        bytes: c.size,
        sizePct: bytesTotal === 0 ? 0 : c.size / bytesTotal * 100,
      }))
      .sort((a, b) => b.bytes - a.bytes)
  })
  const top = () => rows()[0]
  const entryTotal = () => rows().reduce((sum, r) => sum + r.entries, 0)

  return (
    <div class="bg-(--ground) px-6 pt-6 pb-5">
      <div class="flex flex-wrap items-end gap-3.5">
        <Show when={top()} fallback={<span class="font-(family-name:--font-data) text-(length:--text-metric-lg) font-medium leading-(--leading-metric) text-(--ink-4)">Empty</span>}>
          {top => (
            <div class="flex items-baseline gap-[9px] whitespace-nowrap font-(family-name:--font-data)">
              <span class="text-(length:--text-metric-lg) font-medium leading-(--leading-metric) tracking-[-0.04em]" style={{ color: top().color }}>{top().label}</span>
              <span class="text-[20px] text-(--ink-3)">{Math.round(top().sizePct)}%</span>
            </div>
          )}
        </Show>
        <div class="flex items-center gap-2.5 pb-1">
          <span class="text-[15px] font-semibold">Contents</span>
          <span class="text-(length:--text-meta) text-(--ink-4)">{entryTotal()} entries</span>
        </div>
      </div>

      <div class="mt-5 flex h-3 gap-0.5 overflow-hidden rounded-(--radius-bar) bg-(--track)">
        <For each={rows()}>
          {row => <div title={row.label} style={{ width: `${row.sizePct}%`, background: row.color }}/>}
        </For>
      </div>

      <div class={[COLUMNS, 'mt-4 text-[11px] font-semibold uppercase tracking-[0.06em] text-(--ink-5)']}>
        <div>Kind</div>
        <div class="text-right">Entries</div>
        <div class="text-right">% of size</div>
      </div>
      <For each={rows()}>
        {row => (
          <div class={[COLUMNS, 'items-center border-t border-(--track) py-2 font-(family-name:--font-data) text-(length:--text-meta) text-(--ink-2)']}>
            <div class="flex min-w-0 items-center gap-2">
              <span class="size-2.5 shrink-0 rounded-[3px]" style={{ background: row.color }}/>
              <span class="truncate text-(--ink)">{row.label}</span>
            </div>
            <div class="text-right text-(--ink-4)">{row.entries}</div>
            <div class="text-right text-(--ink)">{row.sizePct.toFixed(1)}%</div>
          </div>
        )}
      </For>
    </div>
  )
}
