import { Show } from 'solid-js'
import { useLocation } from '@solidjs/router'
import Autocomplete from './Autocomplete'
import Logo from './Logo'
import { SearchDialog } from './SearchDialog'
import { Icon } from '../icons'
import { theme, toggleTheme } from '../utils/theme'

// below md all three actions collapse to icons on a 44px tap target, rather than into a hamburger
const navItemClass = 'flex cursor-pointer items-center rounded-(--radius-nav) px-2.5 py-1.5 hover:bg-(--track) max-md:size-11 max-md:justify-center max-md:px-0'

export function TopBar() {
  const location = useLocation()
  // the home page has its own hero search field
  const home = () => location.pathname === '/'

  return (
    <>
      <header class="sticky top-0 z-30 flex h-(--header-height) items-center gap-6 border-b border-(--hairline) bg-(--ground)/90 px-7 backdrop-blur-[10px] max-md:gap-2 max-md:px-4">
        <Logo/>
        <Show when={!home()}>
          <Autocomplete class="max-w-[520px] flex-1 max-md:hidden"/>
        </Show>
        <nav class="ml-auto flex items-center gap-2 text-(length:--text-sm) text-(--ink-3)">
          <button type="button" commandfor={SearchDialog.id} command="show-modal"
                  class={[navItemClass, 'md:hidden']} aria-label="Search packages">
            <Icon.Search class="size-[18px]"/>
          </button>
          <a href="https://github.com/harrel56/jar-hell" class={navItemClass} aria-label="GitHub repository">
            <span class="max-md:hidden">GitHub</span>
            <Icon.Github class="size-[18px] md:hidden"/>
          </a>
          <button type="button" class={navItemClass} onClick={toggleTheme}
                  aria-label={theme() === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}>
            <Show when={theme() === 'dark'} fallback={<Icon.Moon class="size-4"/>}>
              <Icon.Sun class="size-4"/>
            </Show>
          </button>
        </nav>
      </header>
      <SearchDialog/>
    </>
  )
}
