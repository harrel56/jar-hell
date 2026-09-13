import { Match, Switch } from 'solid-js'
import { Icon } from '../../../icons'

export type StatusType = 'check' | 'minus' | 'x'

export function StatusDot(props: { type: StatusType }) {
  return (
    <Switch>
      <Match when={props.type === 'check'}><Icon.CircleCheck class="size-[15px] shrink-0 -translate-y-px text-(--good)"/></Match>
      <Match when={props.type === 'minus'}><Icon.CircleMinus class="size-[15px] shrink-0 -translate-y-px text-(--ink-mute)"/></Match>
      <Match when={props.type === 'x'}><Icon.CircleX class="size-[15px] shrink-0 -translate-y-px text-(--ink-mute)"/></Match>
    </Switch>
  )
}
