import { createMemo, For, Show } from 'solid-js'
import { JarInfo } from '../../../api'

interface ContentRow {
  label: string
  entries: number
  bytes: number
  sizePct: number
}

interface FamilyRow extends ContentRow {
  color: string
  sub: SubRow[]
}

interface SubRow extends ContentRow {
  hint: string
}

const CLASS_LABELS: Record<string, string> = {
  SYNTHETIC: 'Synthetic classes',
  UNKNOWN: 'Unknown source',
  INVALID: 'Unreadable classes',
  JRUBY: 'JRuby',
  JYTHON: 'Jython',
  X10: 'X10',
}

const RESOURCE_KINDS: Record<string, { label: string, hint: string }> = {
  NATIVE: { label: 'Native libraries', hint: 'Entries ending .so, .dll, .dylib, .jnilib or .exe, plus extensionless files starting with ELF, PE, Mach-O or ar magic bytes.' },
  ARCHIVE: { label: 'Archives', hint: 'Entries ending .jar, .zip, .tar, .gz and other archive extensions.' },
  XML: { label: 'XML', hint: 'Entries ending .xml, .xsd, .dtd, .xsl, .tld or .wsdl.' },
  JSON: { label: 'JSON', hint: 'Entries ending .json and its variants, .avsc or .geojson.' },
  CONFIG: { label: 'Config', hint: 'Entries ending .properties, .yml, .yaml, .toml, .conf, .ini or .kdl.' },
  WEB: { label: 'Web assets', hint: 'Entries ending .js, .ts, .css, .html and other front-end extensions.' },
  MEDIA: { label: 'Media', hint: 'Images, fonts, audio and video by extension — .png, .svg, .ttf, .woff2, .mp3, .mp4 and alike.' },
  SCRIPT: { label: 'Scripts', hint: 'Entries ending .sh, .bash, .bat, .cmd or .ps1.' },
  TEXT: { label: 'Text', hint: 'Entries ending .txt or .md, plus files named like LICENSE, NOTICE, README or CHANGELOG.' },
  SOURCE: { label: 'Source files', hint: 'Source code of any JVM language shipped in the jar, e.g. .java or .kt.' },
  METADATA: { label: 'Metadata', hint: 'MANIFEST.MF and everything else under META-INF/.' },
  RESOURCE: { label: 'Other', hint: 'Entries that matched no other rule.' },
}

const RESOURCES_LABEL = 'Resources'

const LANGUAGE_COLORS = ['JAVA', 'KOTLIN', 'SCALA', 'GROOVY', 'CLOJURE']

const classLabel = (type: string) =>
  CLASS_LABELS[type] ?? type.charAt(0) + type.slice(1).toLowerCase()

const languageColor = (type: string) =>
  LANGUAGE_COLORS.includes(type) ? `var(--lang-${type.toLowerCase()})` : 'var(--ink-5)'

const byBytes = (a: ContentRow, b: ContentRow) => b.bytes - a.bytes

const COLUMNS = 'grid grid-cols-[minmax(0,1fr)_72px_82px] gap-x-3'

export function JarContents(props: { jar: JarInfo }) {
  const rows = createMemo<FamilyRow[]>(() => {
    const entries = Object.entries(props.jar.contents)
    const bytesTotal = entries.reduce((sum, [, c]) => sum + c.size, 0)
    const pct = (bytes: number) => bytesTotal === 0 ? 0 : bytes / bytesTotal * 100

    const families: FamilyRow[] = []
    const sub: SubRow[] = []
    for (const [type, c] of entries) {
      const kind = RESOURCE_KINDS[type]
      if (kind) {
        sub.push({ label: kind.label, hint: kind.hint, entries: c.count, bytes: c.size, sizePct: pct(c.size) })
      } else {
        families.push({ label: classLabel(type), color: languageColor(type), entries: c.count, bytes: c.size, sizePct: pct(c.size), sub: [] })
      }
    }
    if (sub.length > 0) {
      const bytes = sub.reduce((sum, s) => sum + s.bytes, 0)
      families.push({
        label: RESOURCES_LABEL,
        color: 'var(--lang-resources)',
        entries: sub.reduce((sum, s) => sum + s.entries, 0),
        bytes,
        sizePct: pct(bytes),
        sub: sub.sort(byBytes),
      })
    }
    return families.sort(byBytes)
  })
  const top = () => rows()[0]
  const hero = createMemo(() => {
    const family = top()
    if (!family) return undefined
    const topSub = family.sub[0]
    const subLeads = topSub !== undefined && topSub.bytes / family.bytes >= 0.5
    return {
      label: subLeads ? topSub.label : family.label,
      sizePct: subLeads ? topSub.sizePct : family.sizePct,
      color: family.label === RESOURCES_LABEL ? 'var(--ink)' : family.color,
      note: subLeads ? `in ${family.label.toLowerCase()}` : undefined,
    }
  })
  const entryTotal = () => rows().reduce((sum, r) => sum + r.entries, 0)

  return (
    <div class="bg-(--ground) px-6 pt-6 pb-5">
      <div class="flex flex-wrap items-end gap-3.5">
        <Show when={hero()} fallback={<span class="font-(family-name:--font-data) text-(length:--text-metric-lg) font-medium leading-(--leading-metric) text-(--ink-4)">Empty</span>}>
          {hero => (
            <div class="flex items-baseline gap-[9px] whitespace-nowrap font-(family-name:--font-data)">
              <span
                class={['font-medium leading-(--leading-metric) tracking-[-0.04em]', hero().label.length > 11 ? 'text-[30px]' : 'text-(length:--text-metric-lg)']}
                style={{ color: hero().color }}
              >
                {hero().label}
              </span>
              <span class="text-[20px] text-(--ink-3)">{Math.round(hero().sizePct)}%</span>
              <Show when={hero().note}>
                {note => <span class="font-(family-name:--font-ui) text-[13px] text-(--ink-3)">{note()}</span>}
              </Show>
            </div>
          )}
        </Show>
        <div class="flex items-center gap-2.5 pb-1">
          <span class="text-[15px] font-semibold">Contents</span>
          <span class="text-(length:--text-meta) text-(--ink-4)">{entryTotal()} entries</span>
        </div>
      </div>

      <div class="mt-5 flex h-3 gap-0.5 overflow-hidden rounded-(--radius-bar) bg-(--track)">
        <For each={rows()}>
          {row => <div title={row.label} style={{ width: `${row.sizePct}%`, background: row.color }}/>}
        </For>
      </div>

      <div class={[COLUMNS, 'mt-4 text-[11px] font-semibold uppercase tracking-[0.06em] text-(--ink-5)']}>
        <div>Kind</div>
        <div class="text-right">Entries</div>
        <div class="text-right">% of size</div>
      </div>
      <For each={rows()}>
        {row => (
          <>
            <div class={[COLUMNS, 'items-center border-t border-(--track) py-2 font-(family-name:--font-data) text-(length:--text-meta) text-(--ink-2)']}>
              <div class="flex min-w-0 items-center gap-2" title={row.sub.length > 0 ? `${row.label} — grouped by file kind below` : undefined}>
                <span class="size-2.5 shrink-0 rounded-[3px]" style={{ background: row.color }}/>
                <span class="truncate text-(--ink)">{row.label}</span>
              </div>
              <div class="text-right text-(--ink-4)">{row.entries}</div>
              <div class="text-right text-(--ink)">{row.sizePct.toFixed(1)}%</div>
            </div>
            <For each={row.sub}>
              {sub => (
                <div class={[COLUMNS, 'items-center border-t border-(--track) py-2 font-(family-name:--font-data) text-(length:--text-meta) text-(--ink-2)']}>
                  <div class="flex min-w-0 pl-[18px]" title={sub.hint}>
                    <span class="cursor-help truncate border-b border-dotted border-(--ink-5) text-[12px] text-(--ink-2)">{sub.label}</span>
                  </div>
                  <div class="text-right text-(--ink-4)">{sub.entries}</div>
                  <div class="text-right">{sub.sizePct.toFixed(1)}%</div>
                </div>
              )}
            </For>
          </>
        )}
      </For>
    </div>
  )
}
