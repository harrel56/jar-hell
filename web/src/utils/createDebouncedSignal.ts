import {type Accessor, ComputeFunction, createSignal, onCleanup, type Setter} from 'solid-js'

export function createDebouncedSignal<T>(
  initial: ComputeFunction<undefined | T, T>,
  delayMs: number,
): [value: Accessor<T>, debounced: Accessor<T>, setValue: Setter<T>] {
  const [value, setValue] = createSignal(initial)
  const [debounced, setDebounced] = createSignal<T>(initial)

  let timeoutId: ReturnType<typeof setTimeout> | undefined
  onCleanup(() => clearTimeout(timeoutId))

  const set = ((next: never) => {
    const applied = setValue(next)
    clearTimeout(timeoutId)
    timeoutId = setTimeout(() => setDebounced(() => applied), delayMs)
    return applied
  }) as Setter<T>

  return [value, debounced, set]
}
