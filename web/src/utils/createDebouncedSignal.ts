import { type Accessor, createSignal, onCleanup, type Setter } from 'solid-js'

/* A signal with two readers: `value` updates synchronously — bind it to inputs
   and anything the user should see react instantly — while `debounced` trails
   it by delayMs. Writing again before the delay elapses restarts it, so only
   the latest value ever reaches `debounced`. */
export function createDebouncedSignal<T>(
  initial: Exclude<T, Function>,
  delayMs: number,
): [value: Accessor<T>, debounced: Accessor<T>, setValue: Setter<T>] {
  const [value, setValue] = createSignal<T>(initial)
  const [debounced, setDebounced] = createSignal<T>(initial)

  let timeoutId: ReturnType<typeof setTimeout> | undefined
  onCleanup(() => clearTimeout(timeoutId))

  const set = ((next: never) => {
    const applied = setValue(next)
    clearTimeout(timeoutId)
    /* Functional form: `applied` may itself be a function when T is callable. */
    timeoutId = setTimeout(() => setDebounced(() => applied), delayMs)
    return applied
  }) as Setter<T>

  return [value, debounced, set]
}
