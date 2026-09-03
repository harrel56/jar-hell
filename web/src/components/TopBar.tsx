import Autocomplete from './Autocomplete'
import Logo from './Logo'

const navItemClass = 'cursor-pointer rounded-(--radius-nav) px-2.5 py-1.5 hover:bg-(--track)'

export default function TopBar() {
  return (
    <header class="sticky top-0 z-30 flex h-(--header-height) items-center gap-6 border-b border-(--hairline) bg-(--ground)/90 px-7 backdrop-blur-[10px]">
      <Logo/>
      <Autocomplete/>
      <nav class="ml-auto flex items-center gap-2 text-(length:--text-sm) text-(--ink-3)">
        <a href="/api" class={navItemClass}>API</a>
        <a href="https://github.com/harrel56/jar-hell" class={navItemClass}>GitHub</a>
        {/* Dummy for now — tokens.css already supports data-theme on <html>. */}
        <button type="button" class={navItemClass} aria-label="Toggle theme">☀</button>
      </nav>
    </header>
  )
}
