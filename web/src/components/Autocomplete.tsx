import { createSignal, For, Show } from 'solid-js'

interface Suggestion {
  group: string
  name: string
}

/* Placeholder results until this is wired to /api/v1/packages/search. */
const DUMMY_PACKAGES: Suggestion[] = [
  { group: 'com.sanctionco.jmail', name: 'jmail' },
  { group: 'org.apache.commons', name: 'commons-lang3' },
  { group: 'com.fasterxml.jackson.core', name: 'jackson-databind' },
  { group: 'io.javalin', name: 'javalin' },
  { group: 'org.neo4j.driver', name: 'neo4j-java-driver' },
  { group: 'dev.harrel', name: 'json-schema' },
]

const matches = (query: string) => {
  const q = query.trim().toLowerCase()
  if (!q) return []
  return DUMMY_PACKAGES.filter(p => `${p.group}:${p.name}`.toLowerCase().includes(q))
}

export default function Autocomplete() {
  const [query, setQuery] = createSignal('')
  const [focused, setFocused] = createSignal(false)

  const suggestions = () => matches(query())
  const open = () => focused() && query().trim().length > 0

  const select = (s: Suggestion) => {
    setQuery(`${s.group}:${s.name}`)
    setFocused(false)
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
          <For each={suggestions()}>
            {s => (
              <button
                type="button"
                onClick={() => select(s)}
                class="flex w-full items-baseline gap-[9px] border-b border-(--track) px-3.5 py-[9px] text-left font-(family-name:--font-data) hover:bg-(--surface)"
              >
                <span class="shrink-0 text-(length:--text-label) text-(--ink-5)">{s.group}</span>
                <span class="truncate text-(length:--text-sm) text-(--ink)">{s.name}</span>
              </button>
            )}
          </For>
          <Show when={suggestions().length === 0}>
            <div class="px-3.5 py-3 text-(length:--text-sm) text-(--ink-4)">
              Nothing analysed under that name yet — press Enter to queue it.
            </div>
          </Show>
        </div>
      </Show>
    </div>
  )
}
