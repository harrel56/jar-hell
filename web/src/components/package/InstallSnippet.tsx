import { createMemo, createSignal, For, Show } from 'solid-js'
import { Gav } from '../../utils/gav'

interface InstallSnippetProps {
  gav: Gav
}

type Tool = 'Gradle' | 'Maven'
const TOOLS: Tool[] = ['Gradle', 'Maven']

const gradleSnippet = (gav: Gav) => {
  const classifier = gav.classifier ? `:${gav.classifier}` : ''
  return `implementation '${gav.groupId}:${gav.artifactId}:${gav.version}${classifier}'`
}

const mavenSnippet = (gav: Gav) => {
  const classifier = gav.classifier ? `\n  <classifier>${gav.classifier}</classifier>` : ''
  return `<dependency>
  <groupId>${gav.groupId}</groupId>
  <artifactId>${gav.artifactId}</artifactId>
  <version>${gav.version}</version>${classifier}
</dependency>`
}

export function InstallSnippet(props: InstallSnippetProps) {
  const [tool, setTool] = createSignal<Tool>('Gradle')
  const [copied, setCopied] = createSignal(false)
  const snippet = createMemo(() => tool() === 'Gradle' ? gradleSnippet(props.gav) : mavenSnippet(props.gav))

  const copy = async () => {
    await navigator.clipboard.writeText(snippet())
    setCopied(true)
    setTimeout(() => setCopied(false), 1500)
  }

  return (
    <div class="w-[340px] rounded-(--radius-panel) bg-(--inverse-ground) px-4 py-3.5">
      <div class="mb-2.5 flex gap-4 text-[12px] text-(--ink-4)">
        <For each={TOOLS}>
          {t => (
            <button type="button"
                    onClick={() => setTool(t)}
                    class={['cursor-pointer', {'font-semibold text-white': tool() === t, 'hover:text-(--inverse-ink)': tool() !== t}]}>
              {t}
            </button>
          )}
        </For>
        <button type="button" onClick={copy} class="ml-auto cursor-pointer hover:text-white">
          <Show when={copied()} fallback="copy">copied</Show>
        </button>
      </div>
      <pre class="whitespace-pre-wrap break-all font-(family-name:--font-data) text-[12px] leading-(--leading-code) text-(--inverse-string)">
        {snippet()}
      </pre>
    </div>
  )
}
