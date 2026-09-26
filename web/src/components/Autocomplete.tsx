import {createMemo, createSignal, createUniqueId, Errored, For, latest, onCleanup, Show} from 'solid-js'
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
  variant?: 'header' | 'hero'
  class?: string
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
  const hero = () => props.variant === 'hero'
  let input!: HTMLInputElement

  // `/` focuses the field from anywhere on the page, as the kbd hint promises
  const onSlash = (e: KeyboardEvent) => {
    const target = e.target as HTMLElement | null
    const typing = target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement || target?.isContentEditable
    if (e.key === '/' && !typing && !e.ctrlKey && !e.metaKey && !e.altKey) {
      e.preventDefault()
      input.focus()
    }
  }
  document.addEventListener('keydown', onSlash)
  onCleanup(() => document.removeEventListener('keydown', onSlash))

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
        e.preventDefault()
        if (activeIndex() !== null) {
          document.getElementById(optionId(activeIndex())!)?.click()
        } else if (settledResults().length) {
          document.getElementById(optionId(0)!)?.click()
        } else if (parseGav(query().trim())) {
          setOpened(false)
          navigate('/packages/' + query().trim())
        }
        break
      }
      case 'Escape':
        setOpened(false)
        break
    }
  }

  return (
    <div class={['relative', props.class]}>
      <label class={['flex cursor-text items-center border', {
        'h-9 gap-2.5 rounded-(--radius-field) border-(--hairline-strong) bg-(--surface-sunken) px-[13px] focus-within:border-(--accent)': !hero(),
        'h-[58px] gap-3 rounded-[13px] border-(--hairline-strong) bg-(--ground) px-[18px] focus-within:border-(--ink-mute) focus-within:shadow-(--shadow-field)': hero(),
      }]}>
        <Icon.Search class={`shrink-0 text-(--ink-5) ${hero() ? 'size-[17px]' : 'size-4'}`}/>
        <input
          ref={input}
          autofocus
          value={query()}
          onInput={e => {
            setQuery(e.currentTarget.value)
            setOpened(true)
            setActiveIndex(null)
          }}
          onBlur={() => setOpened(false)}
          onClick={() => setOpened(true)}
          onKeyDown={onKeyDown}
          placeholder={hero() ? 'group:artifact - try json-schema' : 'group:artifact'}
          aria-label="Search packages"
          role="combobox"
          aria-expanded={open() ? 'true' : 'false'}
          aria-controls={listId}
          aria-autocomplete="list"
          aria-activedescendant={optionId(activeIndex())}
          class={['min-w-0 flex-1 border-none bg-transparent font-(family-name:--font-data) text-(--ink) outline-none placeholder:text-(--ink-4)',
            hero() ? 'text-(length:--text-body) tracking-[-0.01em]' : 'text-(length:--text-sm)']}
        />
        <kbd class={['shrink-0 rounded-(--radius-chip) border border-(--hairline) font-(family-name:--font-data) text-(--ink-4)',
          hero() ? 'px-1.5 py-[3px] text-[11px]' : 'px-[5px] py-px text-[10.5px]']}>/</kbd>
      </label>

      <Show when={open()}>
        <div
          id={listId}
          role="listbox"
          onMouseDown={e => e.preventDefault()}
          class={['absolute inset-x-0 z-40 max-h-96 overflow-y-auto border border-(--hairline) bg-(--ground) shadow-(--shadow-menu)',
            hero() ? 'top-[66px] rounded-[13px]' : 'top-11 rounded-(--radius-panel)']}
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
                    'flex cursor-pointer items-baseline gap-[9px] border-b border-(--track) font-(family-name:--font-data)',
                    hero() ? 'px-4 py-[11px]' : 'px-3.5 py-[9px]',
                    activeIndex() === i() ? 'bg-(--surface)' : '',
                  ]}
                >
                  <span class={['shrink-0 text-(--ink-5)', hero() ? 'text-[12px]' : 'text-(length:--text-label)']}>{r.g}</span>
                  <span class={['truncate text-(--ink)', hero() ? 'text-[13.5px]' : 'text-(length:--text-sm)']}>{r.a}</span>
                </a>
              )}
            </For>
            <Show when={results().length === 0}>
              {message('Nothing analysed under that name yet - type the full group:artifact and press Enter to queue it.')}
            </Show>
          </Errored>
        </div>
      </Show>
    </div>
  )
}
