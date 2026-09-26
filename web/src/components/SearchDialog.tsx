import { createEffect } from 'solid-js'
import { useLocation } from '@solidjs/router'
import Autocomplete from './Autocomplete'
import { Icon } from '../icons'

SearchDialog.id = 'search-dialog'

export function SearchDialog() {
  let dialog!: HTMLDialogElement
  const location = useLocation()

  createEffect(() => location.pathname + location.search, () => dialog.close())

  return (
    <dialog
      id={SearchDialog.id}
      ref={dialog}
      closedby="any"
      aria-label="Search packages"
      class="m-0 h-dvh max-h-none w-full max-w-none bg-(--ground) text-(--ink) backdrop:bg-black/35"
    >
      <div onClick={e => (e.target as Element).closest('[role=option]') && dialog.close()}
           class="flex h-13 items-center gap-2 border-b border-(--hairline) px-4">
        <Autocomplete class="min-w-0 flex-1 [&_kbd]:hidden"/>
        <button type="button" commandfor={SearchDialog.id} command="close" aria-label="Close"
                class="flex size-11 shrink-0 cursor-pointer items-center justify-center text-(--ink-4) hover:text-(--ink)">
          <Icon.X class="size-[18px]"/>
        </button>
      </div>
    </dialog>
  )
}
