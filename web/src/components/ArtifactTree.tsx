import {gavToString, getAllDepsCount, isResolvedPackage, Package} from '@/util.ts'
import * as AccordionPrimitive from '@radix-ui/react-accordion'
import {ChevronDown, CircleChevronDown} from 'lucide-react'
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
    return <div className='text-faded py-3'>{gav}</div>
  }

  return (
    <AccordionPrimitive.Root type='multiple' value={openedNodes} onValueChange={onValueChange}
                             className='tree text-left'>
      <AccordionPrimitive.Item value={gav}>
        <AccordionPrimitive.Header className='flex items-center py-3'>
          <AccordionPrimitive.Trigger asChild className='flex transition-all [&[data-state=open]>svg]:rotate-180'>
            <div className='flex items-center gap-2 cursor-pointer'>
              {loading ? <LoadingSpinner/> :
                <CircleChevronDown className='h-4 w-4 shrink-0 transition-transform duration-200' />}
              <span>{gav}</span>
            </div>
          </AccordionPrimitive.Trigger>
        </AccordionPrimitive.Header>
        <AccordionPrimitive.Content asChild
                                    className='overflow-hidden text-sm ml-8 transition-all data-[state=closed]:animate-accordion-up data-[state=open]:animate-accordion-down'>
          <ul>
            {isResolvedPackage(artifact) && artifact.dependencies?.map(dep => (
              <li><DepNode key={gavToString(dep.artifact)} node={dep.artifact}/></li>
            ))}
          </ul>
        </AccordionPrimitive.Content>
      </AccordionPrimitive.Item>
    </AccordionPrimitive.Root>
  )
}