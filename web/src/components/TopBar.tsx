import { Show } from 'solid-js'
import Autocomplete from './Autocomplete'
import Logo from './Logo'
import { Icon } from '../icons'
import { theme, toggleTheme } from '../utils/theme'

const navItemClass = 'cursor-pointer rounded-(--radius-nav) px-2.5 py-1.5 hover:bg-(--track)'

export default function TopBar() {
  return (
    <header class="sticky top-0 z-30 flex h-(--header-height) items-center gap-6 border-b border-(--hairline) bg-(--ground)/90 px-7 backdrop-blur-[10px]">
      <Logo/>
      <Autocomplete/>
      <nav class="ml-auto flex items-center gap-2 text-(length:--text-sm) text-(--ink-3)">
        <a href="/api" class={navItemClass}>API</a>
        <a href="https://github.com/harrel56/jar-hell" class={navItemClass}>GitHub</a>
        <button type="button" class={`${navItemClass} flex items-center`} onClick={toggleTheme}
                aria-label={theme() === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}>
          <Show when={theme() === 'dark'} fallback={<Icon.Moon class="size-4"/>}>
            <Icon.Sun class="size-4"/>
          </Show>
        </button>
      </nav>
    </header>
  )
}
