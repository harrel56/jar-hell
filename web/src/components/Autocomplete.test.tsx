import { cleanup, fireEvent, render, screen } from '@solidjs/testing-library'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import Autocomplete from './Autocomplete'

/* The component's default is 300ms; tests pass a short one so the suite isn't
   dominated by waiting. AFTER_DEBOUNCE must comfortably exceed it. */
const DEBOUNCE_MS = 40
const AFTER_DEBOUNCE = 120
const SEARCH_URL = '/api/v1/packages/search'

const items = [
  { g: 'org.a', a: 'one' },
  { g: 'org.b', a: 'two' },
  { g: 'org.c', a: 'three' },
]

/* Solid flushes asynchronously, so every assertion after an event needs a tick. */
const tick = (ms = 20) => new Promise(r => setTimeout(r, ms))

const ok = (body: unknown) => async () => ({ ok: true, json: async () => body })

const stubFetch = (impl: () => unknown = ok(items)) => {
  const f = vi.fn(impl)
  vi.stubGlobal('fetch', f)
  return f
}

const urls = (f: ReturnType<typeof stubFetch>) =>
  f.mock.calls.map(c => (c as unknown as string[])[0])

const panel = () => screen.queryByRole('listbox')
const options = () => screen.queryAllByRole('option')
const highlighted = () => document.querySelector('[aria-selected="true"]')?.textContent ?? null

const setup = () => {
  render(() => <Autocomplete debounceMs={DEBOUNCE_MS}/>)
  const input = screen.getByLabelText('Search packages') as HTMLInputElement
  fireEvent.focus(input)
  return input
}

const type = (input: HTMLInputElement, value: string) =>
  fireEvent.input(input, { target: { value } })

/** Types and waits for the debounce plus the stubbed response. */
const search = async (input: HTMLInputElement, value: string) => {
  type(input, value)
  await tick(AFTER_DEBOUNCE)
}

const press = async (input: HTMLInputElement, key: string) => {
  fireEvent.keyDown(input, { key })
  await tick()
}

beforeEach(() => stubFetch())
afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('searching', () => {
  test('issues one trimmed, encoded request after the debounce', async () => {
    const f = stubFetch()
    const input = setup()

    await search(input, '  org.test:lib  ')

    expect(f).toHaveBeenCalledTimes(1)
    expect(urls(f)).toEqual([`${SEARCH_URL}?query=org.test%3Alib`])
  })

  test('collapses rapid typing into a single request for the last value', async () => {
    const f = stubFetch()
    const input = setup()

    type(input, 'a')
    await tick(10)
    type(input, 'ab')
    await tick(10)
    type(input, 'abc')
    await tick(AFTER_DEBOUNCE)

    expect(f).toHaveBeenCalledTimes(1)
    expect(urls(f)).toEqual([`${SEARCH_URL}?query=abc`])
  })

  test('does not search for a blank query', async () => {
    const f = stubFetch()
    const input = setup()

    await search(input, '   ')

    expect(f).not.toHaveBeenCalled()
  })

  test('selecting an option does not trigger another search', async () => {
    const f = stubFetch()
    const input = setup()
    await search(input, 'one')

    await press(input, 'ArrowDown')
    await press(input, 'Enter')
    await tick(AFTER_DEBOUNCE)

    expect(f).toHaveBeenCalledTimes(1)
    expect(urls(f)).toEqual([`${SEARCH_URL}?query=one`])
  })

  test('refocusing after a selection does not trigger another search', async () => {
    const f = stubFetch()
    const input = setup()
    await search(input, 'one')
    await press(input, 'ArrowDown')
    fireEvent.keyDown(input, { key: 'Enter' })

    /* Refocus inside the debounce window that select() left running. */
    fireEvent.blur(input)
    input.focus()
    fireEvent.focus(input)
    await tick(AFTER_DEBOUNCE)

    expect(f).toHaveBeenCalledTimes(1)
  })
})

describe('panel visibility', () => {
  test('is closed on mount', () => {
    setup()
    expect(panel()).toBeNull()
  })

  test('stays closed until the debounce settles', async () => {
    const input = setup()

    type(input, 'lib')
    await tick(10)
    expect(panel()).toBeNull()

    await tick(AFTER_DEBOUNCE)
    expect(panel()).toBeInTheDocument()
  })

  test('escape closes it but keeps the text and the focus', async () => {
    const input = setup()
    await search(input, 'lib')

    await press(input, 'Escape')

    expect(panel()).toBeNull()
    expect(input.value).toBe('lib')
    expect(document.activeElement).toBe(input)
  })

  test('blur closes it', async () => {
    const input = setup()
    await search(input, 'lib')

    fireEvent.blur(input)
    await tick()

    expect(panel()).toBeNull()
  })

  test('typing after escape reopens it', async () => {
    const input = setup()
    await search(input, 'lib')
    await press(input, 'Escape')

    await search(input, 'libr')

    expect(panel()).toBeInTheDocument()
  })

  test('refocusing reopens it with the previous results', async () => {
    const f = stubFetch()
    const input = setup()
    await search(input, 'lib')
    fireEvent.blur(input)
    await tick()

    input.focus()
    fireEvent.focus(input)
    await tick()

    expect(options()).toHaveLength(items.length)
    expect(f).toHaveBeenCalledTimes(1)
  })

  test('clearing the input closes it', async () => {
    const input = setup()
    await search(input, 'lib')

    type(input, '')
    await tick()

    expect(panel()).toBeNull()
  })
})

describe('results', () => {
  test('renders an option per result with its group and artifact', async () => {
    const input = setup()

    await search(input, 'o')

    expect(options()).toHaveLength(3)
    expect(options()[0]).toHaveTextContent('org.a')
    expect(options()[0]).toHaveTextContent('one')
    expect(options()[2]).toHaveTextContent('three')
  })

  test('shows the empty state when nothing matches', async () => {
    stubFetch(ok([]))
    const input = setup()

    await search(input, 'nothing')

    expect(options()).toHaveLength(0)
    expect(panel()).toHaveTextContent('Nothing analysed under that name yet')
  })

  test('shows the error state when the request fails', async () => {
    stubFetch(async () => ({ ok: false, status: 500 }))
    const input = setup()

    await search(input, 'boom')

    expect(panel()).toHaveTextContent('Search is unavailable right now.')
  })

  test('recovers from an error on the next successful search', async () => {
    const f = vi.fn()
      .mockImplementationOnce(async () => ({ ok: false, status: 500 }))
      .mockImplementation(ok(items))
    vi.stubGlobal('fetch', f)
    const input = setup()
    await search(input, 'boom')
    expect(panel()).toHaveTextContent('Search is unavailable right now.')

    await search(input, 'fine')

    expect(options()).toHaveLength(items.length)
  })

  test('keeps the previous results on screen while a newer search is in flight', async () => {
    let release: (v: unknown) => void = () => {}
    const f = vi.fn()
      .mockImplementationOnce(ok(items))
      .mockImplementationOnce(() => new Promise(r => (release = r))
        .then(() => ({ ok: true, json: async () => [{ g: 'org.z', a: 'nine' }] })))
    vi.stubGlobal('fetch', f)
    const input = setup()
    await search(input, 'one')

    await search(input, 'nine')
    expect(options()).toHaveLength(items.length)
    expect(options()[0]).toHaveTextContent('org.a')

    release(null)
    await tick(100)
    expect(options()).toHaveLength(1)
    expect(options()[0]).toHaveTextContent('org.z')
  })
})

describe('keyboard navigation', () => {
  test('arrow down from nothing highlights the first option', async () => {
    const input = setup()
    await search(input, 'o')

    await press(input, 'ArrowDown')

    expect(highlighted()).toContain('one')
  })

  test('arrow up from nothing highlights the last option', async () => {
    const input = setup()
    await search(input, 'o')

    await press(input, 'ArrowUp')

    expect(highlighted()).toContain('three')
  })

  test('arrow down past the last option wraps to the first', async () => {
    const input = setup()
    await search(input, 'o')

    await press(input, 'ArrowDown')
    await press(input, 'ArrowDown')
    await press(input, 'ArrowDown')
    await press(input, 'ArrowDown')

    expect(highlighted()).toContain('one')
  })

  test('arrow up from the first option wraps to the last', async () => {
    const input = setup()
    await search(input, 'o')

    await press(input, 'ArrowDown')
    await press(input, 'ArrowUp')

    expect(highlighted()).toContain('three')
  })

  test('enter selects the highlighted option and closes the panel', async () => {
    const input = setup()
    await search(input, 'o')

    await press(input, 'ArrowDown')
    await press(input, 'Enter')

    expect(input.value).toBe('org.a:one')
    expect(panel()).toBeNull()
  })

  test('enter with nothing highlighted leaves the panel alone', async () => {
    const input = setup()
    await search(input, 'o')

    await press(input, 'Enter')

    expect(input.value).toBe('o')
    expect(panel()).toBeInTheDocument()
  })

  test('typing clears the highlight', async () => {
    const input = setup()
    await search(input, 'o')
    await press(input, 'ArrowDown')
    expect(highlighted()).not.toBeNull()

    await search(input, 'on')

    expect(highlighted()).toBeNull()
  })

  test('points aria-activedescendant at the highlighted option', async () => {
    const input = setup()
    await search(input, 'o')

    await press(input, 'ArrowDown')

    expect(input.getAttribute('aria-activedescendant')).toBe(options()[0]!.id)
  })
})

describe('mouse', () => {
  test('clicking an option selects it and closes the panel', async () => {
    const input = setup()
    await search(input, 'o')

    fireEvent.click(options()[1]!)
    await tick()

    expect(input.value).toBe('org.b:two')
    expect(panel()).toBeNull()
  })

  test('moving over an option highlights it', async () => {
    const input = setup()
    await search(input, 'o')

    fireEvent.mouseMove(options()[1]!)
    await tick()

    expect(highlighted()).toContain('two')
  })
})

describe('aria wiring', () => {
  test('marks the input as a combobox controlling the listbox', async () => {
    const input = setup()

    expect(input).toHaveAttribute('role', 'combobox')
    expect(input).toHaveAttribute('aria-autocomplete', 'list')
    expect(input).toHaveAttribute('aria-expanded', 'false')

    await search(input, 'o')

    expect(input).toHaveAttribute('aria-expanded', 'true')
    expect(input.getAttribute('aria-controls')).toBe(panel()!.id)
  })

  test('marks options as unselected until highlighted', async () => {
    const input = setup()
    await search(input, 'o')

    expect(options().every(o => o.getAttribute('aria-selected') === 'false')).toBe(true)

    await press(input, 'ArrowDown')

    expect(options()[0]).toHaveAttribute('aria-selected', 'true')
    expect(options()[1]).toHaveAttribute('aria-selected', 'false')
  })
})
