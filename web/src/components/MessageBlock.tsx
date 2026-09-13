import { JSX } from '@solidjs/web'
import { Icon } from '../icons'

interface MessageBlockProps {
  type: 'warning'
  class?: string | undefined
  children: JSX.Element
}

export function MessageBlock(props: MessageBlockProps) {
  return (
    <div role="alert"
         class={['flex gap-[11px] rounded-[10px] border border-(--note-wash-border) bg-(--note-wash) px-4 py-3 text-(length:--text-sm) text-(--note-ink)', props.class]}>
      <Icon.TriangleAlert class="mt-px size-4 shrink-0 text-(--note-icon)"/>
      <span>{props.children}</span>
    </div>
  )
}
