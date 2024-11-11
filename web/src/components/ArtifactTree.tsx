import {gavToString, getAllDepsCount, isResolvedPackage, Package} from '@/util.ts'
import * as AccordionPrimitive from '@radix-ui/react-accordion'
import {ChevronDown} from 'lucide-react'
import {useFetch} from '@/hooks/useFetch.ts'
import {useState} from 'react'
import {LoadingSpinner} from '@/components/LoadingSpinner.tsx'

export interface PackageTreeProperties {
  artifact: Package
}

export const ArtifactTree = ({artifact}: PackageTreeProperties) => {
  return (
      <DepNode node={artifact}/>
  )
}

const DepNode = ({node}: { node: Package }) => {
  const gav = gavToString(node)
  const [openedNodes, setOpenedNodes] = useState<string[]>([])
  const {data, loading, error, get} = useFetch<Package>(`/api/v1/packages/${gav}?depth=1`, [gav])
  const artifact = data ?? node

  const onValueChange = (opened: string[]) => {
    console.log(opened)
    if (loading) {
      return
    }
    if (isResolvedPackage(artifact) && artifact.dependencies) {
      setOpenedNodes(opened)
    } else {
      get('').then(() => setOpenedNodes(opened))
    }
  }
  if (!isResolvedPackage(artifact) || getAllDepsCount(artifact) === 0) {
    return <div>{gav}</div>
  }

  return (
    <AccordionPrimitive.Root type='multiple' value={openedNodes} onValueChange={onValueChange}>
      <AccordionPrimitive.Item value={gav}>
        <AccordionPrimitive.Header className='flex'>
          <AccordionPrimitive.Trigger
            className='flex flex-1 items-center justify-between py-4 font-medium transition-all [&[data-state=open]>svg]:rotate-180'>
            {loading ? <LoadingSpinner/> :
              <ChevronDown className='h-4 w-4 shrink-0 transition-transform duration-200'/>}
            <span>{gav}</span>
          </AccordionPrimitive.Trigger>
        </AccordionPrimitive.Header>
        <AccordionPrimitive.Content
          className='overflow-hidden text-sm transition-all data-[state=closed]:animate-accordion-up data-[state=open]:animate-accordion-down'>
          {isResolvedPackage(artifact) && artifact.dependencies?.map(dep => (
            <DepNode key={gavToString(dep.artifact)} node={dep.artifact}/>
          ))}
        </AccordionPrimitive.Content>
      </AccordionPrimitive.Item>
    </AccordionPrimitive.Root>
  )
}