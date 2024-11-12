import {Dependency, gavToString, getAllDepsCount, isResolvedPackage, Package} from '@/util.ts'
import * as AccordionPrimitive from '@radix-ui/react-accordion'
import {Circle, CircleAlert, CircleMinus, CirclePlus} from 'lucide-react'
import {useFetch} from '@/hooks/useFetch.ts'
import {forwardRef, useState} from 'react'
import {LoadingSpinner} from '@/components/LoadingSpinner.tsx'
import {clsx} from 'clsx'
import {Link} from 'react-router-dom'
import {Badge} from '@/shadcn/components/ui/Badge.tsx'

export interface PackageTreeProperties {
  artifact: Package
}

export const ArtifactTree = ({artifact}: PackageTreeProperties) => {
  const dummyDep: Dependency = {
    artifact: artifact,
    scope: '',
    optional: false
  }
  return (
    <DepNode node={dummyDep}/>
  )
}

const DepNode = ({node}: { node: Dependency }) => {
  const gav = gavToString(node.artifact)
  const [openedNodes, setOpenedNodes] = useState<string[]>([])
  const {data, loading, error, get} = useFetch<Package>(`/api/v1/packages/${gav}?depth=1`, [gav])
  const artifact = data ?? node.artifact

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
      <div className='py-3'>
        <DepHeader node={node} loading={loading} leaf={true}/>
      </div>
    )
  }

  return (
    <AccordionPrimitive.Root type='multiple' value={openedNodes} onValueChange={onValueChange}
                             className='tree w-full text-left text-sm'>
      <AccordionPrimitive.Item value={gav}>
        <AccordionPrimitive.Header className='flex items-center py-3'>
          <AccordionPrimitive.Trigger asChild>
            <DepHeader node={node} loading={loading}/>
          </AccordionPrimitive.Trigger>
        </AccordionPrimitive.Header>
        <AccordionPrimitive.Content asChild
                                    className='overflow-hidden transition-all data-[state=closed]:animate-accordion-up data-[state=open]:animate-accordion-down'>
          <ul>
            {isResolvedPackage(artifact) && artifact.dependencies?.map(dep => (
              <li key={gavToString(dep.artifact)}>
                <DepNode node={dep}/>
              </li>
            ))}
          </ul>
        </AccordionPrimitive.Content>
      </AccordionPrimitive.Item>
    </AccordionPrimitive.Root>
  )
}

interface DepHeaderProps {
  node: Dependency
  loading: boolean
  leaf?: boolean
}

const DepHeader = forwardRef<HTMLDivElement, DepHeaderProps>(({node, loading, leaf, ...props}, ref) => {
  const gav = gavToString(node.artifact)
  const iconClasses = 'h-4 w-4 shrink-0 transition-all duration-700'
  const getIcon = () => {
    if (loading) {
      return <LoadingSpinner className={iconClasses}/>
    }
    if (!isResolvedPackage(node.artifact)) {
      return <CircleAlert className={clsx(iconClasses, 'text-destructive')}/>
    }
    if (leaf) {
      return <Circle className={iconClasses}/>
    }
    return (
      <>
        <CirclePlus className={clsx(iconClasses, 'cursor-pointer peer plus')}/>
        <CircleMinus className={clsx(iconClasses, 'cursor-pointer minus absolute')}/>
      </>
    )
  }

  const accordionClasses = clsx(
    'relative flex items-center gap-2 transition-all',
    node.optional && 'text-faded',
    '[&[data-state=open]>svg]:rotate-[360deg]',
    '[&[data-state=open]>svg.plus]:opacity-0',
    '[&[data-state=closed]>svg.plus]:opacity-100',
    '[&[data-state=open]>svg.minus]:opacity-100',
    '[&[data-state=closed]>svg.minus]:opacity-0'
  )
  return (
    <div className='flex items-center gap-2'>
      <div ref={ref} {...props} className={accordionClasses} title={isResolvedPackage(node.artifact) ? '' : 'Unresolved package'}>
        {getIcon()}
        <span className='peer-[.cursor-pointer]:cursor-pointer'>{node.artifact.artifactId}</span>
      </div>
      {node.optional && <Badge variant='outline' className='border-border text-faded'>Optional</Badge>}
      <Link to={`/packages/${gav}`}>
        <Badge variant='outline'
               className={clsx('border-primary hover:bg-input', node.optional && 'text-faded border-border')}>
          {node.artifact.version}
        </Badge>
      </Link>
    </div>
  )
})