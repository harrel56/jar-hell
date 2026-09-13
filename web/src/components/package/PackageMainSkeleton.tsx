import { For, Show } from 'solid-js'
import { Gav } from '../../utils/gav'

interface PackageMainSkeletonProps {
  gav: Gav
  /** first analysis of this version, as opposed to plain loading of an analysed one */
  analyzing: boolean
}

interface Bar {
  height: string
  width: string
  delay: string
}

const SECTIONS: { title: string, bars: Bar[] }[] = [
  { title: 'Effective cost', bars: [
    { height: '46px', width: '38%', delay: '0s' },
    { height: '12px', width: '100%', delay: '0.1s' },
    { height: '12px', width: '54%', delay: '0.2s' },
  ] },
  { title: 'Inside this jar', bars: [
    { height: '28px', width: '30%', delay: '0.1s' },
    { height: '12px', width: '86%', delay: '0.2s' },
    { height: '12px', width: '72%', delay: '0.3s' },
    { height: '12px', width: '48%', delay: '0.4s' },
  ] },
  { title: 'Dependency explorer', bars: [
    { height: '12px', width: '64%', delay: '0.15s' },
    { height: '12px', width: '78%', delay: '0.25s' },
    { height: '12px', width: '42%', delay: '0.35s' },
  ] },
]

export function PackageMainSkeleton(props: PackageMainSkeletonProps) {
  return (
    <main class="min-w-0 flex-1 px-10 pt-(--space-10) pb-24">
      <div class="font-(family-name:--font-data) text-(length:--text-sm) text-(--ink-4)">{props.gav.groupId}</div>
      <div class="mt-[5px] flex flex-wrap items-center gap-3">
        <h1 class="font-(family-name:--font-data) text-[36px] font-bold leading-[1.1] tracking-(--tracking-tighter) text-(--ink)">
          {props.gav.artifactId}
        </h1>
        <span class="rounded-[6px] border border-(--accent-wash-border) bg-(--accent-wash) px-[9px] py-[3px] font-(family-name:--font-data) text-[14px] text-(--accent)">
          {props.gav.version}
        </span>
        <Show when={props.analyzing}>
          <span class="flex items-center gap-2 rounded-(--radius-pill) border border-(--accent-wash-border) bg-(--accent-wash) py-1 pr-[11px] pl-[9px] text-[12px] font-semibold uppercase tracking-[0.06em] text-(--accent-hover)">
            <span class="size-[11px] animate-spin rounded-full border-2 border-(--accent-wash-border) border-t-(--accent)"/>
            Analysing
          </span>
        </Show>
      </div>
      <Show when={props.analyzing}>
        <p class="mt-3.5 max-w-[560px] text-(length:--text-lead) text-(--ink-2)">
          This version has not been analysed before. The jar is being unpacked and measured now — the page fills in when the run finishes.
        </p>
        <div class="mt-5 text-(length:--text-meta) text-(--ink-4)">
          Usually under a minute. You can leave and come back — the run continues.
        </div>
      </Show>

      <div class="mt-(--space-11) flex max-w-[900px] flex-col gap-[26px]">
        <For each={SECTIONS}>
          {section => (
            <div>
              <div class="flex items-center gap-3">
                <span class="h-[13px] w-[3px] shrink-0 rounded-[2px] bg-(--hairline-tick)"/>
                <span class="text-[12px] font-semibold uppercase tracking-(--tracking-caps) text-(--ink-mute)">{section.title}</span>
              </div>
              <div class="mt-3 flex flex-col gap-3.5 rounded-(--radius-section) border border-(--hairline-soft) px-6 py-[22px]">
                <For each={section.bars}>
                  {bar => (
                    <div class="animate-pulse rounded-[6px] bg-(--track)"
                         style={{ height: bar.height, width: bar.width, 'animation-delay': bar.delay }}/>
                  )}
                </For>
              </div>
            </div>
          )}
        </For>
      </div>
    </main>
  )
}
