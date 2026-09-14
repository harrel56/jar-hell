import { createMemo, createSignal } from 'solid-js'

export type Theme = 'light' | 'dark'

/** must match the inline script in index.html which applies the theme before first paint */
const STORAGE_KEY = 'theme'

const darkQuery = window.matchMedia('(prefers-color-scheme: dark)')

const readStored = (): Theme | null => {
  const stored = localStorage.getItem(STORAGE_KEY)
  return stored === 'light' || stored === 'dark' ? stored : null
}

const [systemTheme, setSystemTheme] = createSignal<Theme>(darkQuery.matches ? 'dark' : 'light')
darkQuery.addEventListener('change', e => setSystemTheme(e.matches ? 'dark' : 'light'))

/** explicit user choice, `null` means "follow the system" (tokens.css handles that via media query) */
const [preference, setPreference] = createSignal<Theme | null>(readStored())

export const theme = createMemo(() => preference() ?? systemTheme())

export const setTheme = (next: Theme | null) => {
  if (next) {
    localStorage.setItem(STORAGE_KEY, next)
    document.documentElement.dataset['theme'] = next
  } else {
    localStorage.removeItem(STORAGE_KEY)
    delete document.documentElement.dataset['theme']
  }
  setPreference(next)
}

export const toggleTheme = () => setTheme(theme() === 'dark' ? 'light' : 'dark')
