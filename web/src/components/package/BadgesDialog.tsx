import { createMemo, createSignal, For, Show } from 'solid-js'
import { ArtifactInfo } from '../../api'
import { formatGav } from '../../utils/gav'
import { Icon } from '../../icons'

const METRICS = [
  { id: 'size', label: 'package size' },
  { id: 'total_size', label: 'total size' },
  { id: 'bytecode', label: 'package bytecode version' },
  { id: 'effective_bytecode', label: 'effective bytecode version' },
  { id: 'dependencies', label: 'dependencies' },
  { id: 'optional_dependencies', label: 'optional dependencies' },
] as const

type MetricId = typeof METRICS[number]['id']
type Format = 'URL' | 'Markdown' | 'HTML'
const FORMATS: Format[] = ['URL', 'Markdown', 'HTML']

const snippet = (format: Format, badgeUrl: string, pageUrl: string, label: string) => {
  switch (format) {
    case 'URL': return badgeUrl
    case 'Markdown': return `[![${label}](${badgeUrl})](${pageUrl})`
    case 'HTML': return `<a href="${pageUrl}"><img src="${badgeUrl}" alt="${label}"></a>`
  }
}

BadgesDialog.id = 'badges-dialog'

export function BadgesDialog(props: { info: ArtifactInfo }) {
  // a closed dialog is display:none, which does not stop its <img> from loading - render the body
  // only once opened, so the badge endpoint is not hit on every package page view
  const [opened, setOpened] = createSignal(false)
  const [metric, setMetric] = createSignal<MetricId>('total_size')
  const [format, setFormat] = createSignal<Format>('Markdown')
  const [copied, setCopied] = createSignal(false)

  const coordinate = () => formatGav(props.info)
  const label = () => METRICS.find(m => m.id === metric())!.label
  const badgeUrl = createMemo(() => `${location.origin}/api/v1/badges/${metric()}/${coordinate()}`)
  const pageUrl = createMemo(() => `${location.origin}/packages/${coordinate()}`)
  const text = createMemo(() => snippet(format(), badgeUrl(), pageUrl(), label()))

  const copy = async () => {
    await navigator.clipboard.writeText(text())
    setCopied(true)
    setTimeout(() => setCopied(false), 1500)
  }

  return (
    <dialog
      id={BadgesDialog.id}
      closedby="any"
      onToggle={e => setOpened(e.newState === 'open')}
      aria-labelledby="badges-dialog-title"
      class="m-auto w-[640px] max-w-[calc(100vw-32px)] rounded-(--radius-card) bg-(--ground) text-(--ink) shadow-(--shadow-menu) backdrop:bg-black/35"
    >
      <div class="flex items-center gap-3 border-b border-(--hairline) bg-(--surface) px-[22px] py-3">
        <span class="h-[13px] w-[3px] shrink-0 rounded-[2px] bg-(--accent)"/>
        <span id="badges-dialog-title" class="text-[12px] font-semibold uppercase tracking-(--tracking-caps) text-(--ink-4)">Badges</span>
        <button type="button" commandfor={BadgesDialog.id} command="close" aria-label="Close"
                class="ml-auto flex cursor-pointer text-(--ink-4) hover:text-(--ink)">
          <Icon.X class="size-[18px]"/>
        </button>
      </div>

      <div class="px-[22px] pt-5 pb-[22px]">
        <div class="flex flex-wrap gap-2">
          <For each={METRICS}>
            {m => (
              <button type="button" onClick={() => setMetric(m.id)}
                      aria-pressed={metric() === m.id ? 'true' : 'false'}
                      class={['cursor-pointer whitespace-nowrap rounded-(--radius-button) border px-3 py-1.5 text-(length:--text-sm)',
                        metric() === m.id
                          ? 'border-(--ink) bg-(--ink) text-(--ground)'
                          : 'border-(--hairline-strong) text-(--ink-2) hover:border-(--ink)']}>
                {m.label}
              </button>
            )}
          </For>
        </div>

        <Show when={opened()}>
          <img src={badgeUrl()} alt={`${label()} badge`} class="mt-5 h-5"/>
        </Show>

        <div class="mt-5 rounded-(--radius-panel) bg-(--inverse-ground) px-4 py-3.5">
          <div class="mb-2.5 flex gap-4 text-[12px] text-(--ink-4)">
            <For each={FORMATS}>
              {f => (
                <button type="button" onClick={() => setFormat(f)}
                        class={['cursor-pointer', { 'font-semibold text-white': format() === f, 'hover:text-(--inverse-ink)': format() !== f }]}>
                  {f}
                </button>
              )}
            </For>
            <button type="button" onClick={copy} class="ml-auto cursor-pointer hover:text-white">
              <Show when={copied()} fallback="copy">copied</Show>
            </button>
          </div>
          <pre class="whitespace-pre-wrap break-all font-(family-name:--font-data) text-[12px] leading-(--leading-code) text-(--inverse-string)">
            {text()}
          </pre>
        </div>
      </div>
    </dialog>
  )
}
