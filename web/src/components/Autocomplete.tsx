import {createMemo, createSignal, createUniqueId, Errored, For, latest, Show} from 'solid-js'
import {useNavigate, useParams} from '@solidjs/router'
import { createDebouncedSignal } from '../utils/createDebouncedSignal'
import { Icon } from '../icons'
import {parseGav} from '../utils/gav'

interface SearchResult {
  g: string
  a: string
}

const DEBOUNCE_MS = 300

interface AutocompleteProps {
  debounceMs?: number
}

const message = (text: string) => (
  <div class="px-3.5 py-3 text-(length:--text-sm) text-(--ink-4)">{text}</div>
)

export default function Autocomplete(props: AutocompleteProps) {
  const navigate = useNavigate()
  const params = useParams()
  const gav = () => parseGav(params['coordinate'])
  const [query, debouncedQuery, setQuery] = createDebouncedSignal(() => gav() ? params['coordinate']! : '', props.debounceMs ?? DEBOUNCE_MS)
  const [opened, setOpened] = createSignal(false)
  const [activeIndex, setActiveIndex] = createSignal<number | null>(() => (opened(), null))

  const listId = createUniqueId()
  const optionId = (i: number | null) => i === null ? undefined : `${listId}-opt-${i}`

  const results = createMemo(async (prev): Promise<SearchResult[]> => {
    const q = debouncedQuery().trim()
    if (!opened() || !q) {
      return prev
    }
    const res = await fetch(`/api/v1/packages/search?query=${encodeURIComponent(q)}`)
    if (!res.ok) {
      throw new Error('Searching for packages failed')
    }
    return res.json()
  }, {loadingValue: []})

  const packagePath = (r: SearchResult) => `/packages/${r.g}:${r.a}`

  const select = (r: SearchResult) => {
    setQuery(`${r.g}:${r.a}`)
    setOpened(false)
  }

  const hasQuery = () => query().trim().length > 0 && debouncedQuery().trim().length > 0
  const open = () => opened() && hasQuery()
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
    document.getElementById(optionId(idx)!)?.scrollIntoView?.({ block: 'nearest', behavior: 'auto'})
  }

  const onKeyDown = (e: KeyboardEvent) => {
    switch (e.key) {
      case 'ArrowDown':
      case 'ArrowUp':
        e.preventDefault()
        if (open()) {
          moveActiveIndex(e.key === 'ArrowDown' ? 1 : -1)
        } else {
          setOpened(true)
        }
        break
      case 'Enter': {
        const idx = activeIndex()
        if (idx === null) {
          setOpened(false)
          navigate('/packages/' + query())
        } else {
          const option = document.getElementById(optionId(idx)!)
          if (option) {
            e.preventDefault()
            option.click()
          }
        }
        break
      }
      case 'Escape':
        setOpened(false)
        break
    }
  }

  return (
    <div class="relative max-w-[520px] flex-1">
      <label class="flex h-9 cursor-text items-center gap-2.5 rounded-(--radius-field) border border-(--hairline-strong) bg-(--surface-sunken) px-[13px] focus-within:border-(--accent)">
        <input
          value={query()}
          onInput={e => {
            setQuery(e.currentTarget.value)
            setOpened(true)
            setActiveIndex(null)
          }}
          onBlur={() => setOpened(false)}
          onClick={() => setOpened(true)}
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
        <Icon.Search class="size-4 shrink-0 text-(--ink-4)"/>
      </label>

      <Show when={open()}>
        <div
          id={listId}
          role="listbox"
          onMouseDown={e => e.preventDefault()}
          class="absolute inset-x-0 top-11 z-40 max-h-96 overflow-y-auto rounded-(--radius-panel) border border-(--hairline) bg-(--ground) shadow-(--shadow-menu)"
        >
          <Errored fallback={() => message('Search is unavailable right now.')}>
            <For each={results()}>
              {(r, i) => (
                <a
                  id={optionId(i())}
                  href={packagePath(r)}
                  role="option"
                  tabindex={-1}
                  aria-selected={activeIndex() === i() ? 'true' : 'false'}
                  onClick={e => {
                    if (e.button === 0 && !e.ctrlKey && !e.metaKey && !e.shiftKey && !e.altKey) {
                      select(r)
                    }
                  }}
                  onMouseMove={() => setActiveIndex(i())}
                  class={[
                    'flex cursor-pointer items-baseline gap-[9px] border-b border-(--track) px-3.5 py-[9px] font-(family-name:--font-data)',
                    activeIndex() === i() ? 'bg-(--surface)' : '',
                  ]}
                >
                  <span class="shrink-0 text-(length:--text-label) text-(--ink-5)">{r.g}</span>
                  <span class="truncate text-(length:--text-sm) text-(--ink)">{r.a}</span>
                </a>
              )}
            </For>
            <Show when={results().length === 0}>
              {message('Nothing analysed under that name yet — press Enter to queue it.')}
            </Show>
          </Errored>
        </div>
      </Show>
    </div>
  )
}
