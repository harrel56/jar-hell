import {Separator} from '@/components/ui/Separator.tsx'
import React, {useMemo} from 'react'
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/Accordion.tsx'
import {useParams} from 'react-router-dom'
import {stringToGav} from '@/util.ts'
import clsx from 'clsx'

export interface VersionPickerProps {
  versions: string[]
}

export const VersionPicker = ({versions}: VersionPickerProps) => {
  const { gav } = useParams()
  const gavObject = useMemo(() => stringToGav(gav!), [gav])
  const versionNodes = useMemo(() => calculateVersionNodes(versions), [versions])


  return (
    <Accordion type='single' collapsible className='w-full min-w-[160px] basis-1/5'>
      <h2 className='mb-4 text-2xl font-bold'>Versions</h2>
      {Array.from(versionNodes.entries()).map(([major, versions]) => (
        <AccordionItem key={major} value={major}>
          <AccordionTrigger>
            <div>
              <span>{`${major}.x`}</span>
              <span className='ml-4 text-muted text-xs'>{`${versions.length} items`}</span>
            </div>
          </AccordionTrigger>
          <AccordionContent>
            {versions.map(version =>
              <React.Fragment key={version}>
                <Separator className='my-1 mx-4'/>
                <div className={clsx('text-sm font-mono ml-4 py-1.5 px-2 rounded-sm transition-colors hover:bg-input',
                  version === gavObject?.version && 'bg-input text-hellyeah')}>
                  {version}
                </div>
              </React.Fragment>
            )}
          </AccordionContent>
        </AccordionItem>

      ))}
    </Accordion>
  )
}

/**
 * Group by minor version if:
 * - there is only 1 major
 * - in scope of 1 major there is a minor that got >= 10 patch versions
 * otherwise group by major
 * */
const calculateVersionNodes = (versions: string[]) => {
  const byMajor = new Map<string, Map<string, string[]>>()
  versions.forEach(version => {
    const [major, minor] = version.split('.', 2)
    const byMinor = byMajor.get(major) ?? new Map<string, string[]>()
    const patches = byMinor.get(minor) ?? []
    patches.push(version)
    byMinor.set(minor, patches)
    byMajor.set(major, byMinor)
  })

  const nodes = new Map<string, string[]>()
  Array.from(byMajor.entries()).forEach(([major, byMinor]) => {
    const expandMinor = byMajor.size === 1 || Array.from(byMinor.values()).some(patches => patches.length >= 10)
    if (expandMinor) {
      Array.from(byMinor.entries()).forEach(([minor, patches]) => {
        nodes.set(`${major}.${minor}`, patches)
      })
    } else {
      const newVersions: string[] = []
      Array.from(byMinor.values()).forEach(patches => newVersions.push(...patches))
      nodes.set(major, newVersions)
    }
  })
  return nodes
}
