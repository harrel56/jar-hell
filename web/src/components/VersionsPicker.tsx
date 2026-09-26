import { createMemo, For, Show } from 'solid-js'
import { ArtifactVersion } from '../api'
import { Gav } from '../utils/gav'
import { calculateVersionNodes, versionHref } from '../utils/versions'
import { VersionStatePill } from './VersionStatePill'
import { Icon } from '../icons'

interface VersionsPickerProps {
  gav: Gav
  versions: readonly ArtifactVersion[]
}

VersionsPicker.id = 'versions-dialog'

export function VersionsPicker(props: VersionsPickerProps) {
  let dialog!: HTMLDialogElement
  const nodes = createMemo(() => Array.from(calculateVersionNodes(props.versions.toReversed()).entries()))

  return (
    <>
      <button type="button" commandfor={VersionsPicker.id} command="show-modal"
              class="flex cursor-pointer items-center gap-2 rounded-[6px] border border-(--accent-wash-border) bg-(--accent-wash) px-[9px] py-[5px] font-(family-name:--font-data) text-[14px] text-(--accent)">
        {props.gav.version}
        <Icon.ChevronDown class="size-2.5"/>
      </button>

      <dialog
        id={VersionsPicker.id}
        ref={dialog}
        closedby="any"
        aria-labelledby="versions-dialog-title"
        class="m-0 h-dvh max-h-none w-full max-w-none bg-(--ground) text-(--ink) backdrop:bg-black/35"
      >
        <div class="flex h-full flex-col">
          <div class="flex shrink-0 items-center gap-3 border-b border-(--hairline) bg-(--surface) px-4 py-3">
            <span class="h-[13px] w-[3px] shrink-0 rounded-[2px] bg-(--accent)"/>
            <span id="versions-dialog-title" class="text-[12px] font-semibold uppercase tracking-(--tracking-caps) text-(--ink-4)">Versions</span>
            <span class="font-(family-name:--font-data) text-(length:--text-label) text-(--ink-5)">{props.versions.length}</span>
            <button type="button" commandfor={VersionsPicker.id} command="close" aria-label="Close"
                    class="ml-auto flex cursor-pointer text-(--ink-4) hover:text-(--ink)">
              <Icon.X class="size-[18px]"/>
            </button>
          </div>

          <div class="min-h-0 flex-1 overflow-y-auto overscroll-contain">
            <For each={nodes()}>
              {([label, versions]) => (
                <section>
                  <div class="flex items-center gap-2.5 border-b border-(--hairline-soft) bg-(--surface) px-4 py-2.5">
                    <span class="font-(family-name:--font-data) text-(length:--text-meta) font-medium text-(--ink-3)">{`${label}.x`}</span>
                    <span class="font-(family-name:--font-data) text-(length:--text-label) text-(--ink-5)">{versions.length}</span>
                  </div>
                  <For each={versions}>
                    {version => (
                      // navigating keeps the same dialog element mounted, so it has to be closed by hand
                      <a href={versionHref(props.gav, version.version)} onClick={() => dialog.close()}
                         class={['flex items-center gap-2.5 border-b border-l-2 border-b-(--track) px-4 py-3 font-(family-name:--font-data) text-(length:--text-body)',
                           version.version === props.gav.version
                             ? 'border-l-(--accent) bg-(--surface-sunken) text-(--accent)'
                             : 'border-l-transparent text-(--ink-2)']}>
                        {version.version}
                        <VersionStatePill state={version.state}/>
                        <Show when={version.version === props.gav.version}>
                          <Icon.CircleCheck class="ml-auto size-4 shrink-0"/>
                        </Show>
                      </a>
                    )}
                  </For>
                </section>
              )}
            </For>
          </div>
        </div>
      </dialog>
    </>
  )
}
