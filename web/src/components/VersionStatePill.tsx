import { Match, Switch } from 'solid-js'
import { ArtifactVersion } from '../api'

const pillClass = 'rounded-(--radius-pill) border px-1.5 py-0.5 font-semibold uppercase tracking-[0.04em] text-(length:--text-micro)'

interface VersionStatePillProps {
  state: ArtifactVersion['state']
  class?: string | undefined
}

export function VersionStatePill(props: VersionStatePillProps) {
  return (
    <Switch>
      <Match when={props.state === 'ANALYZED'}>
        <span class={[pillClass, 'border-(--good-wash-border) bg-(--good-wash) text-(--good-wash-ink)', props.class]}>Analyzed</span>
      </Match>
      <Match when={props.state === 'FAILED'}>
        <span class={[pillClass, 'border-(--bad-wash-border) bg-(--bad-wash) text-(--bad-wash-ink)', props.class]}>Failed</span>
      </Match>
    </Switch>
  )
}
