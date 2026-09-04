import {createMemo, createSignal, Errored, For, Loading, Show} from 'solid-js'
import { createDebouncedSignal } from '../utils/createDebouncedSignal'

interface SearchResult {
  g: string
  a: string
}

const DEBOUNCE_MS = 200

const message = (text: string) => (
  <div class="px-3.5 py-3 text-(length:--text-sm) text-(--ink-4)">{text}</div>
)

export default function Autocomplete() {
  const [query, debouncedQuery, setQuery] = createDebouncedSignal('', DEBOUNCE_MS)
  const [focused, setFocused] = createSignal(false)

  const results = createMemo(async (): Promise<SearchResult[]> => {
    const q = debouncedQuery().trim()
    if (!q) {
      return []
    }
    const res = await fetch(`/api/v1/packages/search?query=${encodeURIComponent(q)}`)
    if (!res.ok) {
      throw new Error("Searching for packages failed")
    }
    return res.json()
  })

  const select = (r: SearchResult) => {
    setQuery(`${r.g}:${r.a}`)
    setFocused(false)
  }

  const open = () => focused() && query().trim().length > 0
  const isDebouncing = () => query().trim() !== debouncedQuery().trim()

  return (
    <div class="relative max-w-[520px] flex-1">
      <div
        class={[
          'flex h-9 items-center gap-2.5 rounded-(--radius-field) border bg-(--surface-sunken) px-[13px]',
          focused() ? 'border-(--accent)' : 'border-(--hairline-strong)',
        ]}
      >
        <input
          value={query()}
          onInput={e => setQuery(e.currentTarget.value)}
          onFocus={() => setFocused(true)}
          /* Deferred so a click on a suggestion lands before the menu unmounts. */
          onBlur={() => setTimeout(() => setFocused(false), 120)}
          onKeyDown={e => e.key === 'Escape' && setFocused(false)}
          placeholder="group:artifact"
          aria-label="Search packages"
          class="min-w-0 flex-1 border-none bg-transparent font-(family-name:--font-data) text-(length:--text-sm) text-(--ink) outline-none placeholder:text-(--ink-4)"
        />
        <span class="shrink-0 text-(length:--text-meta) text-(--ink-4)">⌕</span>
      </div>

      <Show when={open()}>
        <div class="absolute inset-x-0 top-11 z-40 overflow-hidden rounded-(--radius-panel) border border-(--hairline) bg-(--ground) shadow-(--shadow-menu)">
          <Errored fallback={() => message('Search is unavailable right now.')}>
            <Show when={!isDebouncing()} fallback={message('Searching…')}>
              <Loading fallback={message('Searching…')}>
                <For each={results()}>
                  {r => (
                    <button
                      type="button"
                      onClick={() => select(r)}
                      class="flex w-full items-baseline gap-[9px] border-b border-(--track) px-3.5 py-[9px] text-left font-(family-name:--font-data) hover:bg-(--surface)"
                    >
                      <span class="shrink-0 text-(length:--text-label) text-(--ink-5)">{r.g}</span>
                      <span class="truncate text-(length:--text-sm) text-(--ink)">{r.a}</span>
                    </button>
                  )}
                </For>
                <Show when={results().length === 0}>
                  {message('Nothing analysed under that name yet — press Enter to queue it.')}
                </Show>
              </Loading>
            </Show>
          </Errored>
        </div>
      </Show>
    </div>
  )
}
