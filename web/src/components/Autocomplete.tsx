import {createMemo, createSignal, createUniqueId, Errored, For, latest, Show, untrack} from 'solid-js'
import {useParams} from '@solidjs/router'
import { createDebouncedSignal } from '../utils/createDebouncedSignal'
import { Icon } from '../icons'

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

const parseCoordinate = (coordinate: string | undefined) => {
  if (!coordinate) {
    return ''
  }
  const parts = coordinate.split(':')
  if (parts.length === 2) {
    return coordinate
  } else if (parts.length === 3) {
    return parts[0] + ':' + parts[1]
  } else {
    return ''
  }
}

export default function Autocomplete(props: AutocompleteProps) {
  const params = useParams()
  const [query, debouncedQuery, setQuery] = createDebouncedSignal(() => parseCoordinate(params['coordinate']), props.debounceMs ?? DEBOUNCE_MS)
  const [focused, setFocused] = createSignal(false)
  const [dismissed, setDismissed] = createSignal(false)
  const [activeIndex, setActiveIndex] = createSignal<number | null>(null)

  const listId = createUniqueId()
  const optionId = (i: number | null) => i === null ? undefined : `${listId}-opt-${i}`

  const results = createMemo(async (prev): Promise<SearchResult[]> => {
    const q = debouncedQuery().trim()
    if (untrack(dismissed) || !q) {
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
    setDismissed(true)
    setActiveIndex(null)
  }

  const open = () => focused() && !dismissed() && query().trim().length > 0 && debouncedQuery().trim().length > 0
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
          const option = document.getElementById(optionId(idx)!)
          if (option) {
            e.preventDefault()
            option.click()
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
      <label
        class={[
          'flex h-9 cursor-text items-center gap-2.5 rounded-(--radius-field) border bg-(--surface-sunken) px-[13px]',
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
          onClick={() => {
            setFocused(true)
            setDismissed(false)
          }}
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
