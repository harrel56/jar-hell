import { createMemo, For } from 'solid-js'
import { JarInfo } from '../../../api'
import { StatusDot } from './StatusDot'

interface Fact {
  label: string
  value: string
  note: string
  on: boolean
}

const MODULE_LABELS = { NAMED: 'Named module', AUTOMATIC: 'Automatic module', UNNAMED: 'Unnamed' }

const facts = (jar: JarInfo): Fact[] => [
  {
    label: 'JPMS module',
    value: MODULE_LABELS[jar.moduleType],
    note: jar.moduleName ?? 'no module name declared',
    on: jar.moduleType !== 'UNNAMED',
  },
  {
    label: 'Built with',
    value: jar.buildJdk ? `JDK ${jar.buildJdk}` : 'Unknown JDK',
    note: jar.buildJdk ? 'as declared in manifest' : 'no information in manifest',
    on: Boolean(jar.buildJdk),
  },
  {
    label: 'Multi-release',
    value: jar.multiReleaseJar ? 'Yes' : 'No',
    note: jar.multiReleaseJar ? 'versioned classes under META-INF/versions' : 'single class set for all JDKs',
    on: jar.multiReleaseJar,
  },
  {
    label: 'Entry point',
    value: jar.executable ? 'Present' : 'None',
    note: jar.executable ? 'runnable with java -jar' : 'library only, not executable',
    on: jar.executable,
  },
]

export function JarFacts(props: { jar: JarInfo }) {
  const rows = createMemo(() => facts(props.jar))

  return (
    <div class="grid grid-cols-[repeat(auto-fit,minmax(210px,1fr))] gap-px border-t border-(--hairline) bg-(--hairline)">
      <For each={rows()}>
        {fact => (
          <div class="min-w-0 bg-(--ground) px-5 pt-[15px] pb-4">
            <div class="text-(length:--text-label) font-semibold uppercase tracking-[0.06em] text-(--ink-5)">{fact.label}</div>
            <div class="mt-[7px] flex min-w-0 items-center gap-2">
              <StatusDot type={fact.on ? 'check' : 'minus'}/>
              <span class={['truncate font-(family-name:--font-data) text-(length:--text-body)', fact.on ? 'text-(--ink)' : 'text-(--ink-3)']}>
                {fact.value}
              </span>
            </div>
            <div class="mt-[5px] text-[12px] text-(--ink-4)">{fact.note}</div>
          </div>
        )}
      </For>
    </div>
  )
}
