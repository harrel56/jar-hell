import { createSignal, For, onCleanup, Show } from 'solid-js'

/* Shape of PackagesController.SearchResult — deliberately terse over the wire. */
interface SearchResult {
  g: string
  a: string
}

const DEBOUNCE_MS = 200

export default function Autocomplete() {
  const [query, setQuery] = createSignal('')
  const [results, setResults] = createSignal<SearchResult[]>([])
  const [loading, setLoading] = createSignal(false)
  const [failed, setFailed] = createSignal(false)
  const [focused, setFocused] = createSignal(false)

  let debounceId: ReturnType<typeof setTimeout> | undefined
  let inFlight: AbortController | undefined

  onCleanup(() => {
    clearTimeout(debounceId)
    inFlight?.abort()
  })

  const search = async (q: string) => {
    inFlight?.abort()
    if (!q) {
      setResults([])
      setLoading(false)
      return
    }
    const controller = new AbortController()
    inFlight = controller
    setLoading(true)
    setFailed(false)
    try {
      const res = await fetch(`/api/v1/packages/search?query=${encodeURIComponent(q)}`, {
        signal: controller.signal,
      })
      if (!res.ok) {
        throw new Error(`search failed with ${res.status}`)
      }
      setResults(await res.json())
    } catch {
      /* An abort means a newer query took over — leave its state alone. */
      if (controller.signal.aborted) {
        return
      }
      setResults([])
      setFailed(true)
    } finally {
      if (!controller.signal.aborted) {
        setLoading(false)
      }
    }
  }

  const onInput = (value: string) => {
    setQuery(value)
    clearTimeout(debounceId)
    debounceId = setTimeout(() => void search(value.trim()), DEBOUNCE_MS)
  }

  const select = (r: SearchResult) => {
    setQuery(`${r.g}:${r.a}`)
    setFocused(false)
  }

  const open = () => focused() && query().trim().length > 0

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
          onInput={e => onInput(e.currentTarget.value)}
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
            <div class="px-3.5 py-3 text-(length:--text-sm) text-(--ink-4)">
              <Show when={!loading()} fallback="Searching…">
                <Show when={!failed()} fallback="Search is unavailable right now.">
                  Nothing analysed under that name yet — press Enter to queue it.
                </Show>
              </Show>
            </div>
          </Show>
        </div>
      </Show>
    </div>
  )
}
