import {Separator} from '@/components/ui/Separator.tsx'
import {useMemo} from 'react'
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/Accordion.tsx'

export interface VersionPickerProps {
  versions: string[]
}

export const VersionPicker = ({versions}: VersionPickerProps) => {
  const versionNodes: Map<string, string[]> = useMemo(() => {
    const byMajor = new Map<string, string[]>()
    versions.forEach(version => {
      const [major] = version.split('.', 1)
      const newVersions = byMajor.get(major) ?? []
      newVersions.push(version);
      byMajor.set(major, newVersions)
    })
    return byMajor
  }, [versions])

  return (
    <Accordion type="single" collapsible className="w-full">
      <h2 className="mb-4 text-2xl font-bold">Versions</h2>
      {Array.from(versionNodes.entries()).map(([major, versions]) => (
        <AccordionItem key={major} value={major}>
          <AccordionTrigger>{major}</AccordionTrigger>
          <AccordionContent>
            {versions.map(version =>
              <>
                <div key={version} className="text-sm font-mono">
                  {version}
                </div>
                <Separator key={`separator-${version}`} className="my-2"/>
              </>
            )}
          </AccordionContent>
        </AccordionItem>

      ))}
    </Accordion>
  )
}
