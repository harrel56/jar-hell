import { Show } from 'solid-js'
import { ArtifactInfo } from '../../api'
import { formatDate, formatDateTime } from '../../utils/utils'

interface AnalysisFailedViewProps {
  info: ArtifactInfo
  onRetry: () => void
}

/** server stops re-analysing an artifact after this many failed attempts */
const MAX_ATTEMPTS = 10

const cellClass = 'bg-(--ground) px-5 pt-3.5 pb-[15px]'
const cellLabelClass = 'font-semibold uppercase tracking-[0.06em] text-(length:--text-label) text-(--ink-5)'
const cellValueClass = 'font-(family-name:--font-data) text-[15px] text-(--ink)'

export function AnalysisFailedView(props: AnalysisFailedViewProps) {
  const attempts = () => props.info.unresolvedCount ?? 1
  const givenUp = () => attempts() >= MAX_ATTEMPTS
  const reason = () => props.info.unresolvedReason ?? 'Unknown error'
  const reportUrl = () => {
    const title = `Analysis failed: ${props.info.groupId}:${props.info.artifactId}:${props.info.version}`
    return `https://github.com/harrel56/jar-hell/issues/new?title=${encodeURIComponent(title)}`
  }

  return (
    <main class="min-w-0 flex-1 px-10 pt-(--space-10) pb-24">
      <div class="font-(family-name:--font-data) text-(length:--text-sm) text-(--ink-4)">{props.info.groupId}</div>
      <div class="mt-[5px] flex flex-wrap items-center gap-3">
        <h1 class="font-(family-name:--font-data) text-[36px] font-bold leading-[1.1] tracking-(--tracking-tighter) text-(--ink)">
          {props.info.artifactId}
        </h1>
        <span class="rounded-[6px] border border-(--accent-wash-border) bg-(--accent-wash) px-[9px] py-[3px] font-(family-name:--font-data) text-[14px] text-(--accent)">
          {props.info.version}
        </span>
        <span class="flex items-center gap-2 rounded-(--radius-pill) border border-(--bad-wash-border) bg-(--bad-wash) py-1 px-[11px] text-[12px] font-semibold uppercase tracking-[0.06em] text-(--bad-wash-ink)">
          <span class="size-[9px] shrink-0 rounded-full bg-(--bad-wash-ink)"/>
          Analysis failed
        </span>
      </div>
      <p class="mt-3.5 max-w-[560px] text-(length:--text-lead) text-(--ink-2)">
        Jarhell fetched this version but could not measure it. No metrics are available.
      </p>

      <div class="mt-[26px] max-w-[760px] overflow-hidden rounded-(--radius-section) border border-(--hairline)">
        <div class="grid grid-cols-[repeat(auto-fit,minmax(180px,1fr))] gap-px bg-(--hairline)">
          <div class={cellClass}>
            <div class={cellLabelClass}>Last attempt</div>
            <div class={`mt-1.5 ${cellValueClass}`}>
              <Show when={props.info.analyzed} fallback="—">
                {analyzed => <span title={formatDateTime(analyzed())}>{formatDate(analyzed())}</span>}
              </Show>
            </div>
          </div>
          <div class={cellClass}>
            <div class={cellLabelClass}>Attempts</div>
            <div class="mt-1.5 flex items-baseline gap-2">
              <span class={cellValueClass}>{attempts()}</span>
              <Show when={givenUp()}>
                <span class="text-(length:--text-meta) text-(--ink-4)">no further automatic retries</span>
              </Show>
            </div>
          </div>
        </div>

        <div class="flex items-baseline gap-2.5 border-t border-(--hairline) bg-(--surface) px-5 py-3">
          <span class="shrink-0 text-(length:--text-sm) font-semibold text-(--ink)">Reason</span>
          <span class="min-w-0 break-all font-(family-name:--font-data) text-[12px] text-(--ink-3)">{reason()}</span>
        </div>
      </div>

      <div class="mt-5 flex flex-wrap gap-2 text-(length:--text-sm)">
        <button type="button"
                disabled={givenUp()}
                onClick={() => props.onRetry()}
                class="flex items-center gap-1.75 rounded-(--radius-button) bg-(--ink) px-3.5 py-2 text-(--ground) enabled:cursor-pointer enabled:hover:bg-(--ink-2) disabled:opacity-50">
          Try again
        </button>
        <a href={reportUrl()}
           class="flex items-center gap-1.75 rounded-(--radius-button) border border-(--hairline-strong) px-3.5 py-2 text-(--ink) hover:border-(--ink)">
          Report a problem
        </a>
      </div>
    </main>
  )
}
