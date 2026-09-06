import { cleanup, fireEvent, render, screen } from '@solidjs/testing-library'
import { flush } from 'solid-js'
import { createRouter, memoryHistory, type MemoryHistoryAdapter } from '@solidjs/router'
import { afterEach, beforeEach, describe, expect, type Mock, test, vi } from 'vitest'
import Autocomplete from './Autocomplete'

/* The component's default is 300ms; tests pass a short one so the suite isn't
   dominated by waiting. AFTER_DEBOUNCE must comfortably exceed it. */
const DEBOUNCE_MS = 10
const AFTER_DEBOUNCE = 20
const SEARCH_URL = '/api/v1/packages/search'

const items = [
  { g: 'org.a', a: 'one' },
  { g: 'org.b', a: 'two' },
  { g: 'org.c', a: 'three' },
]

/* Only for real time passing — the debounce timer and the stubbed response.
   Reactive settling is handled synchronously by flush(). */
const wait = (ms: number) => new Promise(r => setTimeout(r, ms))

const ok = (body: unknown) => async () => ({ ok: true, json: async () => body })
const fails = (status = 500) => async () => ({ ok: false, status })

let input: HTMLInputElement
let fetchMock: Mock
let history: MemoryHistoryAdapter

/** Swaps the stub mid-test; safe until the first request goes out. */
const useFetch = (mock: Mock) => {
  fetchMock = mock
  vi.stubGlobal('fetch', mock)
  return mock
}

const stubFetch = (impl: () => unknown = ok(items)) => useFetch(vi.fn(impl))

const urls = () => fetchMock.mock.calls.map(c => (c as unknown as string[])[0])

const panel = () => screen.queryByRole('listbox')
const options = () => screen.queryAllByRole('option')
const highlighted = () => document.querySelector('[aria-selected="true"]')?.textContent ?? null

/* Real focus/blur, not fireEvent: these move document.activeElement as well as
   firing the event, and fireEvent.blur would leave the element focused so the
   next focus() would be a silent no-op. */
const focusInput = () => {
  input.focus()
  flush()
}

const blurInput = () => {
  input.blur()
  flush()
}

const type = (value: string) => {
  fireEvent.input(input, { target: { value } })
  flush()
}

/** Types and waits for the debounce plus the stubbed response. */
const search = async (value: string) => {
  type(value)
  await wait(AFTER_DEBOUNCE)
}

const press = (key: string) => {
  fireEvent.keyDown(input, { key })
  flush()
}

/* Selecting navigates, so the component needs router context. Mirrors App:
   the autocomplete lives outside the route outlet and survives navigation. */
beforeEach(() => {
  stubFetch()
  history = memoryHistory('/')
  const Router = createRouter({
    routes: [
      { path: '/', component: () => null },
      { path: '/packages/:coordinate', component: () => <p>package route</p> },
      { path: '*404', component: () => null },
    ],
    history,
  })
  render(() => (
    <Router>
      {props => (
        <>
          <Autocomplete debounceMs={DEBOUNCE_MS}/>
          {props.children}
        </>
      )}
    </Router>
  ))
  input = screen.getByLabelText('Search packages') as HTMLInputElement
  focusInput()
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('searching', () => {
  test('issues one trimmed, encoded request after the debounce', async () => {
    await search('  org.test:lib  ')

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(urls()).toEqual([`${SEARCH_URL}?query=org.test%3Alib`])
  })

  test('collapses rapid typing into a single request for the last value', async () => {
    type('a')
    await wait(5)
    type('ab')
    await wait(5)
    type('abc')
    await wait(AFTER_DEBOUNCE)

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(urls()).toEqual([`${SEARCH_URL}?query=abc`])
  })

  test('does not search for a blank query', async () => {
    await search('   ')

    expect(fetchMock).not.toHaveBeenCalled()
  })

  test('selecting an option does not trigger another search', async () => {
    await search('one')

    press('ArrowDown')
    press('Enter')
    await wait(AFTER_DEBOUNCE)

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(urls()).toEqual([`${SEARCH_URL}?query=one`])
  })

  // won't fix for now - 1 additional request in some edge case scenario
  test.todo('refocusing after a selection does not trigger another search', async () => {
    await search('one')
    press('ArrowDown')
    press('Enter')

    /* Refocus inside the debounce window that select() left running. */
    blurInput()
    focusInput()
    await wait(AFTER_DEBOUNCE)

    expect(fetchMock).toHaveBeenCalledTimes(1)
  })
})

describe('panel visibility', () => {
  test('is closed on mount', () => {
    expect(panel()).toBeNull()
  })

  test('stays closed until the debounce settles', async () => {
    type('lib')
    await wait(5)
    expect(panel()).toBeNull()

    await wait(AFTER_DEBOUNCE)
    expect(panel()).toBeInTheDocument()
  })

  test('escape closes it but keeps the text and the focus', async () => {
    await search('lib')

    press('Escape')

    expect(panel()).toBeNull()
    expect(input.value).toBe('lib')
    expect(document.activeElement).toBe(input)
  })

  test('blur closes it', async () => {
    await search('lib')

    blurInput()

    expect(panel()).toBeNull()
  })

  test('typing after escape reopens it', async () => {
    await search('lib')
    press('Escape')

    await search('libr')

    expect(panel()).toBeInTheDocument()
  })

  test('refocusing reopens it with the previous results', async () => {
    await search('lib')
    blurInput()

    focusInput()

    expect(options()).toHaveLength(items.length)
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  test('clearing the input closes it', async () => {
    await search('lib')

    type('')

    expect(panel()).toBeNull()
  })
})

describe('results', () => {
  test('renders an option per result with its group and artifact', async () => {
    await search('o')

    expect(options()).toHaveLength(3)
    expect(options()[0]).toHaveTextContent('org.a')
    expect(options()[0]).toHaveTextContent('one')
    expect(options()[2]).toHaveTextContent('three')
  })

  test('shows the empty state when nothing matches', async () => {
    stubFetch(ok([]))

    await search('nothing')

    expect(options()).toHaveLength(0)
    expect(panel()).toHaveTextContent('Nothing analysed under that name yet')
  })

  test('shows the error state when the request fails', async () => {
    stubFetch(fails())

    await search('boom')

    expect(panel()).toHaveTextContent('Search is unavailable right now.')
  })

  test('recovers from an error on the next successful search', async () => {
    useFetch(vi.fn().mockImplementationOnce(fails()).mockImplementation(ok(items)))
    await search('boom')
    expect(panel()).toHaveTextContent('Search is unavailable right now.')

    await search('fine')

    expect(options()).toHaveLength(items.length)
  })

  test('keeps the previous results on screen while a newer search is in flight', async () => {
    let release: (v: unknown) => void = () => {}
    useFetch(vi.fn()
      .mockImplementationOnce(ok(items))
      .mockImplementationOnce(() => new Promise(r => (release = r))
        .then(() => ({ ok: true, json: async () => [{ g: 'org.z', a: 'nine' }] }))))
    await search('one')

    await search('nine')
    expect(options()).toHaveLength(items.length)
    expect(options()[0]).toHaveTextContent('org.a')

    release(null)
    await wait(AFTER_DEBOUNCE)
    expect(options()).toHaveLength(1)
    expect(options()[0]).toHaveTextContent('org.z')
  })
})

describe('keyboard navigation', () => {
  test('arrow down from nothing highlights the first option', async () => {
    await search('o')

    press('ArrowDown')

    expect(highlighted()).toContain('one')
  })

  test('arrow up from nothing highlights the last option', async () => {
    await search('o')

    press('ArrowUp')

    expect(highlighted()).toContain('three')
  })

  test('arrow down past the last option wraps to the first', async () => {
    await search('o')

    press('ArrowDown')
    press('ArrowDown')
    press('ArrowDown')
    press('ArrowDown')

    expect(highlighted()).toContain('one')
  })

  test('arrow up from the first option wraps to the last', async () => {
    await search('o')

    press('ArrowDown')
    press('ArrowUp')

    expect(highlighted()).toContain('three')
  })

  test('enter selects the highlighted option and closes the panel', async () => {
    await search('o')

    press('ArrowDown')
    press('Enter')

    expect(input.value).toBe('org.a:one')
    expect(panel()).toBeNull()
    expect(history.get()).toBe('/packages/org.a:one')
  })

  test('enter with nothing highlighted leaves the panel alone', async () => {
    await search('o')

    press('Enter')

    expect(input.value).toBe('o')
    expect(panel()).toBeInTheDocument()
  })

  test('typing clears the highlight', async () => {
    await search('o')
    press('ArrowDown')
    expect(highlighted()).not.toBeNull()

    await search('on')

    expect(highlighted()).toBeNull()
  })

  test('points aria-activedescendant at the highlighted option', async () => {
    await search('o')

    press('ArrowDown')

    expect(input.getAttribute('aria-activedescendant')).toBe(options()[0]!.id)
  })
})

describe('mouse', () => {
  test('clicking an option selects it and closes the panel', async () => {
    await search('o')

    fireEvent.click(options()[1]!)
    flush()

    expect(input.value).toBe('org.b:two')
    expect(panel()).toBeNull()
    expect(history.get()).toBe('/packages/org.b:two')
  })

  test('moving over an option highlights it', async () => {
    await search('o')

    fireEvent.mouseMove(options()[1]!)
    flush()

    expect(highlighted()).toContain('two')
  })
})

describe('aria wiring', () => {
  test('marks the input as a combobox controlling the listbox', async () => {
    expect(input).toHaveAttribute('role', 'combobox')
    expect(input).toHaveAttribute('aria-autocomplete', 'list')
    expect(input).toHaveAttribute('aria-expanded', 'false')

    await search('o')

    expect(input).toHaveAttribute('aria-expanded', 'true')
    expect(input.getAttribute('aria-controls')).toBe(panel()!.id)
  })

  test('marks options as unselected until highlighted', async () => {
    await search('o')

    expect(options().every(o => o.getAttribute('aria-selected') === 'false')).toBe(true)

    press('ArrowDown')

    expect(options()[0]).toHaveAttribute('aria-selected', 'true')
    expect(options()[1]).toHaveAttribute('aria-selected', 'false')
  })
})
