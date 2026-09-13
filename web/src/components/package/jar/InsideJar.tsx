import { ArtifactInfo, JarInfo } from '../../../api'
import { JarContents } from './JarContents'
import { PublicApi } from './PublicApi'
import { JarFacts } from './JarFacts'
import { PublishedAlongside } from './PublishedAlongside'

interface InsideJarProps {
  info: ArtifactInfo
  jar: JarInfo
}

const fileName = (info: ArtifactInfo) => {
  const classifier = info.classifier ? `-${info.classifier}` : ''
  return `${info.artifactId}-${info.version}${classifier}.${info.packaging ?? 'jar'}`
}

export function InsideJar(props: InsideJarProps) {
  return (
    <section class="@container mt-(--space-11) overflow-hidden rounded-(--radius-section) border border-(--hairline)">
      <div class="flex flex-wrap items-center gap-x-3 gap-y-2.5 border-b border-(--hairline) bg-(--surface) px-[22px] py-3">
        <span class="h-[13px] w-[3px] shrink-0 rounded-[2px] bg-(--accent)"/>
        <span class="whitespace-nowrap text-[12px] font-semibold uppercase tracking-(--tracking-caps) text-(--ink-4)">Inside this jar</span>
        <span class="text-(length:--text-meta) text-(--ink-5)">{fileName(props.info)} only - dependencies excluded</span>
      </div>
      <div class="grid grid-cols-[repeat(auto-fit,minmax(360px,1fr))] gap-px bg-(--hairline)">
        <JarContents jar={props.jar}/>
        <PublicApi jar={props.jar}/>
      </div>
      <JarFacts jar={props.jar}/>
      <PublishedAlongside info={props.info}/>
    </section>
  )
}
