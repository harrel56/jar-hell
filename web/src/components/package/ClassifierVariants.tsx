import { createMemo, For, Show } from 'solid-js'
import { ArtifactInfo } from '../../api'
import { formatGav } from '../../utils/gav'

interface ClassifierVariantsProps {
  info: ArtifactInfo
}

interface Variant {
  classifier: string | undefined
  coordinate: string
}

const HIDDEN_CLASSIFIERS = new Set(['sources', 'javadoc'])

export function ClassifierVariants(props: ClassifierVariantsProps) {
  const variants = createMemo<Variant[]>(() => {
    const classifiers = (props.info.classifiers ?? []).filter(c => !HIDDEN_CLASSIFIERS.has(c))
    if (props.info.classifier && !classifiers.includes(props.info.classifier)) {
      classifiers.push(props.info.classifier)
    }
    if (classifiers.length === 0) return []
    const base = { groupId: props.info.groupId, artifactId: props.info.artifactId, version: props.info.version }
    return [undefined, ...classifiers.sort()].map(classifier => ({
      classifier,
      coordinate: formatGav(classifier ? { ...base, classifier } : base),
    }))
  })

  return (
    <Show when={props.info.classifier || variants().length > 0}>
      <div class="mt-(--space-9) border-t border-(--hairline) pt-(--space-7)">
        <Show when={props.info.classifier}>
          <div class="mb-4 max-w-[560px] rounded-r-[9px] border border-l-[3px] border-(--accent-wash-border) border-l-(--accent) bg-(--accent-wash) px-[15px] py-3">
            <div class="text-[13.5px] font-semibold text-(--ink)">You are viewing a classifier artifact</div>
            <div class="mt-1 text-(length:--text-meta) leading-(--leading-body) text-(--ink-2)">
              Everything below describes the <span class="font-(family-name:--font-data)">{props.info.classifier}</span> variant, not the main artifact.
            </div>
          </div>
        </Show>
        <Show when={variants().length > 0}>
          <div class="flex flex-wrap items-center gap-x-3.5 gap-y-2.5">
            <div class="whitespace-nowrap text-[12px] font-semibold uppercase tracking-(--tracking-caps) text-(--ink-4)">Variants</div>
            <div class="flex flex-wrap gap-2">
              <For each={variants()}>
                {v => {
                  const selected = () => (props.info.classifier || undefined) === v.classifier
                  return (
                    <a href={`/packages/${v.coordinate}`} title={v.coordinate}
                       class={[
                         'flex items-center gap-[9px] rounded-(--radius-field) border px-3 py-[7px] hover:border-(--ink)',
                         selected() ? 'border-(--accent-wash-border) bg-(--accent-wash)' : 'border-(--hairline-strong) bg-(--ground)',
                       ]}>
                      <span class={['font-(family-name:--font-data) text-(length:--text-sm)', selected() ? 'text-(--accent)' : 'text-(--ink)']}>
                        {v.classifier ?? 'main artifact'}
                      </span>
                      <span class={['text-(length:--text-label)', selected() ? 'text-(--accent-hover)' : 'text-(--ink-3)']}>
                        {v.classifier ? 'classifier' : 'no classifier'}
                      </span>
                    </a>
                  )
                }}
              </For>
            </div>
          </div>
        </Show>
      </div>
    </Show>
  )
}
