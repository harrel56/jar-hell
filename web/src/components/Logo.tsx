/* The wordmark: a dark rounded tile holding the accent glyph, then "Jarhell"
   with the second half in accent. */
export default function Logo() {
  return (
    <a
      href="/"
      class="flex items-center gap-[9px] font-bold text-(length:--text-brand) tracking-(--tracking-tighter) text-(--ink)"
    >
      <span class="flex size-6 items-center justify-center rounded-(--radius-nav) bg-(--ink)">
        <span
          class="h-[11px] w-[9px] bg-(--accent-mark)"
          style={{ 'clip-path': 'polygon(50% 0%, 100% 62%, 78% 100%, 22% 100%, 0% 62%)' }}
        />
      </span>
      <span>
        Jar<span class="text-(--accent)">hell</span>
      </span>
    </a>
  )
}
