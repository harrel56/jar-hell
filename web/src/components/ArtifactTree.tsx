import {gavToString, getAllDepsCount, isResolvedPackage, Package} from '@/util.ts'
import * as AccordionPrimitive from '@radix-ui/react-accordion'
import {Circle, CircleChevronDown, CircleMinus, CirclePlus} from 'lucide-react'
import {useFetch} from '@/hooks/useFetch.ts'
import {forwardRef, TransitionEventHandler, useRef, useState} from 'react'
import {LoadingSpinner} from '@/components/LoadingSpinner.tsx'
import {clsx} from 'clsx'

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
    return (
      <div className='text-faded py-3'>
        <DepHeader node={artifact} loading={loading} leaf={true}/>
      </div>
    )
  }

  return (
    <AccordionPrimitive.Root type='multiple' value={openedNodes} onValueChange={onValueChange}
                             className='tree text-left'>
      <AccordionPrimitive.Item value={gav}>
        <AccordionPrimitive.Header className='flex items-center py-3'>
          <AccordionPrimitive.Trigger asChild>
            <DepHeader node={artifact} loading={loading}/>
          </AccordionPrimitive.Trigger>
        </AccordionPrimitive.Header>
        <AccordionPrimitive.Content asChild
                                    className='overflow-hidden text-sm ml-8 transition-all data-[state=closed]:animate-accordion-up data-[state=open]:animate-accordion-down'>
          <ul>
            {isResolvedPackage(artifact) && artifact.dependencies?.map(dep => (
              <li key={gavToString(dep.artifact)}>
                <DepNode node={dep.artifact}/>
              </li>
            ))}
          </ul>
        </AccordionPrimitive.Content>
      </AccordionPrimitive.Item>
    </AccordionPrimitive.Root>
  )
}

interface DepHeaderProps {
  node: Package
  loading: boolean
  leaf?: boolean
}

const DepHeader = forwardRef<HTMLDivElement, DepHeaderProps>(({node, loading, leaf, ...props}, ref) => {
  const iconClasses = 'h-4 w-4 shrink-0 transition-all duration-700'
  const getIcon = () => {
    if (loading) {
      return <LoadingSpinner className={iconClasses}/>
    }
    if (leaf) {
      return <Circle className={iconClasses}/>
    }
    return (
      <>
        <CirclePlus className={clsx(iconClasses, 'plus')}/>
        <CircleMinus className={clsx(iconClasses, 'minus absolute')}/>
      </>
    )
  }

  return (
    <div ref={ref} {...props} className='relative flex items-center gap-2 cursor-pointer transition-all [&[data-state=open]>svg]:rotate-[360deg]
     [&[data-state=open]>svg.plus]:opacity-0 [&[data-state=closed]>svg.plus]:opacity-100
     [&[data-state=open]>svg.minus]:opacity-100 [&[data-state=closed]>svg.minus]:opacity-0'>
      {getIcon()}
      <span>{gavToString(node)}</span>
    </div>
  )
})