import {createMemo, createSignal, createUniqueId, Errored, For, latest, Loading, Show} from 'solid-js'
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
  const [dismissed, setDismissed] = createSignal(false)
  const [activeIndex, setActiveIndex] = createSignal<number | null>(null)

  const listId = createUniqueId()
  const optionId = (i: number | null) => i === null ? undefined : `${listId}-opt-${i}`

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

  const open = () => focused() && !dismissed() && query().trim().length > 0
  const isDebouncing = () => query().trim() !== debouncedQuery().trim()

  const select = (r: SearchResult) => {
    setQuery(`${r.g}:${r.a}`)
    setDismissed(true)
    setActiveIndex(null)
  }

  const settledResults = () => (open() ? latest(results) : [])

  const moveActiveIndex = (delta: number) => {
    const count = settledResults().length
    if (count === 0 || delta === 0) {
      return
    }
    const idx = setActiveIndex(prev => {
      if (prev === null) {
        return delta > 0 ? 0 : count - 1
      }
      const idx = (prev + delta) % count
      return idx >= 0 ? idx : count + idx
    })
    document.getElementById(optionId(idx)!)?.scrollIntoView?.({ block: 'nearest', behavior: 'smooth'})
  }

  const onKeyDown = (e: KeyboardEvent) => {
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault()
        moveActiveIndex(1)
        break
      case 'ArrowUp':
        e.preventDefault()
        moveActiveIndex(-1)
        break
      case 'Enter': {
        const idx = activeIndex()
        if (idx !== null) {
          const active = settledResults()[idx]
          if (active) {
            e.preventDefault()
            select(active)
          }
        }
        /* With nothing highlighted Enter falls through — that is the
           "press Enter to queue it" path, still to be implemented. */
        break
      }
      case 'Escape':
        setDismissed(true)
        setActiveIndex(null)
        break
    }
  }

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
          onInput={e => {
            setQuery(e.currentTarget.value)
            setDismissed(false)
            setActiveIndex(null)
          }}
          onFocus={() => {
            setFocused(true)
            setDismissed(false)
          }}
          onBlur={() => setFocused(false)}
          onKeyDown={onKeyDown}
          placeholder="group:artifact"
          aria-label="Search packages"
          role="combobox"
          aria-expanded={open() ? 'true' : 'false'}
          aria-controls={listId}
          aria-autocomplete="list"
          aria-activedescendant={optionId(activeIndex())}
          class="min-w-0 flex-1 border-none bg-transparent font-(family-name:--font-data) text-(length:--text-sm) text-(--ink) outline-none placeholder:text-(--ink-4)"
        />
        <span class="shrink-0 text-(length:--text-meta) text-(--ink-4)">⌕</span>
      </div>

      <Show when={open()}>
        <div
          id={listId}
          role="listbox"
          onMouseDown={e => e.preventDefault()}
          class="absolute inset-x-0 top-11 z-40 max-h-96 overflow-y-auto rounded-(--radius-panel) border border-(--hairline) bg-(--ground) shadow-(--shadow-menu)"
        >
          <Errored fallback={() => message('Search is unavailable right now.')}>
            <Show when={!isDebouncing()} fallback={message('Searching…')}>
              <Loading fallback={message('Searching…')}>
                <For each={results()}>
                  {(r, i) => (
                    <div
                      id={optionId(i())}
                      role="option"
                      aria-selected={activeIndex() === i() ? 'true' : 'false'}
                      onClick={() => select(r)}
                      onMouseMove={() => setActiveIndex(i())}
                      class={[
                        'flex cursor-pointer items-baseline gap-[9px] border-b border-(--track) px-3.5 py-[9px] font-(family-name:--font-data)',
                        activeIndex() === i() ? 'bg-(--surface)' : '',
                      ]}
                    >
                      <span class="shrink-0 text-(length:--text-label) text-(--ink-5)">{r.g}</span>
                      <span class="truncate text-(length:--text-sm) text-(--ink)">{r.a}</span>
                    </div>
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
