import { createSignal, For } from 'solid-js'
import { Icon } from '../../../icons'

export function Services(props: { services: string[] }) {
  const [opened, setOpened] = createSignal(false)
  const empty = () => props.services.length === 0

  return (
    <div class="border-t border-(--hairline) bg-(--surface)">
      <button type="button"
              aria-expanded={opened() ? 'true' : 'false'}
              disabled={empty()}
              class="flex w-full items-center gap-3 px-[22px] py-[13px] text-left enabled:cursor-pointer enabled:hover:bg-(--track)"
              onClick={() => setOpened(o => !o)}>
        <span class="text-(length:--text-sm) font-semibold">Services provided</span>
        <span class="font-(family-name:--font-data) text-(length:--text-sm) text-(--ink-2)">{props.services.length}</span>
        <span class="text-(length:--text-meta) text-(--ink-4)">
          {empty() ? 'No META-INF/services entries' : 'declared in META-INF/services'}
        </span>
        <span class={['ml-auto text-(--ink-5) transition-transform', { 'rotate-180': opened() }, { invisible: empty() }]}>
          <Icon.ChevronDown class="size-2.5"/>
        </span>
      </button>
      <div class={['grid transition-[grid-template-rows] duration-(--duration-rail) ease-(--ease-rail)',
        { 'grid-rows-[0fr]': !opened(), 'grid-rows-[1fr]': opened() }]} inert={!opened()}>
        <div class="overflow-hidden px-[22px]">
          <For each={props.services}>
            {service => (
              <div class="border-t border-(--hairline) py-[9px] font-(family-name:--font-data) text-(length:--text-meta) text-(--ink)">
                <span class="block truncate">{service}</span>
              </div>
            )}
          </For>
          <div class="h-3.5"/>
        </div>
      </div>
    </div>
  )
}
