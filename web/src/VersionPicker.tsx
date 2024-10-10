import {Separator} from '@/components/ui/Separator.tsx'
import React, {useMemo} from 'react'
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/Accordion.tsx'
import {Link, useParams} from 'react-router-dom'
import {Package, stringToGav} from '@/util.ts'
import clsx from 'clsx'
import {Badge} from '@/components/ui/Badge.tsx'

export interface VersionPickerProps {
  versions: string[]
  analyzedPackages: Package[]
}

interface VersionNode {
  version: string
  package?: Package
}

export const VersionPicker = ({versions, analyzedPackages}: VersionPickerProps) => {
  const { gav } = useParams()
  const gavObject = useMemo(() => stringToGav(gav!), [gav])
  const versionNodes = useMemo(() => calculateVersionNodes(versions, analyzedPackages),
    [versions, analyzedPackages])
  const defaultSeries = useMemo(() => {
    if (!gavObject.version) {
      return undefined
    }
    const [major, minor] = gavObject.version.split('.', 2)
    return versionNodes.has(major) ? major : `${major}.${minor}`
  }, [versionNodes, gavObject])

  return (
    <Accordion type='single' collapsible defaultValue={defaultSeries} className='w-full min-w-[160px] basis-1/5'>
      <h2 className='mb-4 text-2xl font-bold'>Versions</h2>
      {Array.from(versionNodes.entries()).map(([versionSeries, versions]) => (
        <AccordionItem key={versionSeries} value={versionSeries}>
          <AccordionTrigger>
            <div>
              <span className={clsx(versionSeries === defaultSeries && 'text-hellyeah')}>{`${versionSeries}.x`}</span>
              <span className='ml-4 text-input text-xs'>{`${versions.length} items`}</span>
            </div>
          </AccordionTrigger>
          <AccordionContent>
            {versions.map(node =>
              <React.Fragment key={node.version}>
                <Separator className='my-1 mx-4'/>
                <Link className={clsx('block text-sm font-mono ml-4 py-1.5 px-2 rounded-sm transition-colors hover:bg-input',
                  node.version === gavObject?.version && 'bg-input text-hellyeah')}
                  to={`/packages/${gavObject?.groupId}:${gavObject?.artifactId}:${node.version}`}>
                  {node.version}
                  {node.package && <Badge variant='outline' className='ml-4 border-primary'>Analyzed</Badge>}
                </Link>
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
const calculateVersionNodes = (versions: string[], analyzedPackages: Package[]) => {
  const byMajor = new Map<string, Map<string, string[]>>()
  versions.forEach(version => {
    const [major, minor] = version.split('.', 2)
    const byMinor = byMajor.get(major) ?? new Map<string, string[]>()
    const patches = byMinor.get(minor) ?? []
    patches.push(version)
    byMinor.set(minor, patches)
    byMajor.set(major, byMinor)
  })

  const nodes = new Map<string, VersionNode[]>()
  Array.from(byMajor.entries()).forEach(([major, byMinor]) => {
    const expandMinor = byMajor.size === 1 || Array.from(byMinor.values()).some(patches => patches.length >= 10)
    if (expandMinor) {
      Array.from(byMinor.entries()).forEach(([minor, patches]) => {
        nodes.set(`${major}.${minor}`, joinVersionsWithPackages(patches, analyzedPackages))
      })
    } else {
      const newVersions: string[] = []
      Array.from(byMinor.values()).forEach(patches => newVersions.push(...patches))
      nodes.set(major, joinVersionsWithPackages(newVersions, analyzedPackages))
    }
  })
  return nodes
}

const joinVersionsWithPackages = (versions: string[], analyzedPackages: Package[]) => {
  return versions.map(version => ({version, package: analyzedPackages.find(pkg => pkg.version === version)}))
}
