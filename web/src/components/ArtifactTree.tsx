import {gavToString, getAllDepsCount, isResolvedPackage, Package} from '@/util.ts'
import * as AccordionPrimitive from '@radix-ui/react-accordion'
import {ChevronDown} from 'lucide-react'
import {useFetch} from '@/hooks/useFetch.ts'
import {useState} from 'react'

export interface PackageTreeProperties {
  artifact: Package
}

export const ArtifactTree = ({artifact}: PackageTreeProperties) => {
  const gav = gavToString(artifact)
  return (
    <AccordionPrimitive.Root type='multiple'>
      <AccordionPrimitive.Item value={gav}>
        <AccordionPrimitive.Header className='flex'>
          <AccordionPrimitive.Trigger
            className='flex flex-1 items-center justify-between py-4 font-medium transition-all [&[data-state=open]>svg]:rotate-180'>
            <ChevronDown className='h-4 w-4 shrink-0 transition-transform duration-200'/>
            <span>{gav}</span>
          </AccordionPrimitive.Trigger>
        </AccordionPrimitive.Header>
        <AccordionPrimitive.Content
          className='overflow-hidden text-sm transition-all data-[state=closed]:animate-accordion-up data-[state=open]:animate-accordion-down'>
          <AccordionPrimitive.Root type='multiple'>
            {isResolvedPackage(artifact) && artifact.dependencies.map(dep => (
              <DepNode key={gavToString(dep.artifact)} node={dep.artifact}/>
            ))}
          </AccordionPrimitive.Root>
        </AccordionPrimitive.Content>
      </AccordionPrimitive.Item>
    </AccordionPrimitive.Root>
  )
}

const DepNode = ({node}: { node: Package }) => {
  const gav = gavToString(node)
  const [openedNodes, setOpenedNodes] = useState([])
  const {data, loading, error, get} = useFetch<Package>(`/api/v1/packages/${gav}?depth=1`, [gav])
  const artifact = data ?? node
  const hasLoadedDeps = isResolvedPackage(artifact) && getAllDepsCount(artifact) === artifact.dependencies.length

  const onOpen = () => {

  }

  return (
    <AccordionPrimitive.Item value={gav}>
      <AccordionPrimitive.Header className='flex'>
        <AccordionPrimitive.Trigger
          className='flex flex-1 items-center justify-between py-4 font-medium transition-all [&[data-state=open]>svg]:rotate-180'>
          <ChevronDown className='h-4 w-4 shrink-0 transition-transform duration-200'
                       onClick={() => get('')}
          />
          <span>{gav}</span>
        </AccordionPrimitive.Trigger>
      </AccordionPrimitive.Header>
      <AccordionPrimitive.Content
        className='overflow-hidden text-sm transition-all data-[state=closed]:animate-accordion-up data-[state=open]:animate-accordion-down'>
        <AccordionPrimitive.Root type='multiple'>
          {isResolvedPackage(artifact) && artifact.dependencies.map(dep => (
            <DepNode key={gavToString(dep.artifact)} node={dep.artifact}/>
          ))}
        </AccordionPrimitive.Root>
      </AccordionPrimitive.Content>
    </AccordionPrimitive.Item>
  )
}