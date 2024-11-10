import {gavToString, Package} from '@/util.ts'
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/shadcn/components/ui/Accordion.tsx'

export interface PackageTreeProperties {
  artifact: Package
}

export const ArtifactTree = ({artifact}: PackageTreeProperties) => {
  return <DepNode artifact={artifact}/>
}

const DepNode = ({artifact}: { artifact: Package }) => {
  const nodeName = gavToString(artifact)
  return (
    <Accordion type='multiple'>
      <AccordionItem value={nodeName}>
        <AccordionTrigger>
          <span>{nodeName}</span>
        </AccordionTrigger>
        {!artifact.unresolved && artifact.dependencies && (
          <AccordionContent>
            {artifact.dependencies.map(dep => <DepNode key={gavToString(dep.artifact)} artifact={dep.artifact}/>)}
          </AccordionContent>
        )}
      </AccordionItem>
    </Accordion>
  )
}