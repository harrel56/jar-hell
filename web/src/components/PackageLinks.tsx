import {PropsWithChildren} from 'react'
import {Button} from '@/shadcn/components/ui/Button.tsx'
import {Gav, ResolvedPackage} from '@/util.ts'
import {BugIcon, FileCheck, HouseIcon} from 'lucide-react'
import {clsx} from 'clsx'
import {Icons} from '@/shadcn/components/ui/Icons.tsx'

export const PackageLinks = ({pkg}: {pkg: ResolvedPackage}) => {
  return (
    <div className='flex gap-1'>
      <ExternalLink href={pkg.url}>
        <HouseIcon/>
      </ExternalLink>
      <ExternalLink href={pkg.scmUrl}>
        {getScmIcon(pkg.scmUrl)}
      </ExternalLink>
      <ExternalLink href={pkg.issuesUrl}>
        <BugIcon/>
      </ExternalLink>
      <ExternalLink href={pkg.classifiers.includes('javadoc') ? generateJavaDocUrl(pkg) : undefined}>
        <FileCheck/>
      </ExternalLink>
    </div>
  )
}

const ExternalLink = ({href, children}: PropsWithChildren<{ href?: string }>) => {
  return (
    <a href={href} target='_blank' rel='noreferrer'>
      <Button variant='ghost'
              size='icon-lg'
              disabled={!href}
              className={clsx(!href && 'text-faded')}>
        {children}
      </Button>
    </a>
  )
}

const getScmIcon = (url?: string) => {
  const iconClass = 'h-[1.5rem] w-[1.5rem]'
  if (!url) {
    return <Icons.Git className={iconClass}/>
  } else if (url.includes('github.com')) {
    return <Icons.GitHub className={iconClass}/>
  } else if (url.includes('gitlab.com')) {
    return <Icons.GitLab className={iconClass}/>
  } else if (url.includes('bitbucket.org')) {
    return <Icons.BitBucket className={iconClass}/>
  } else {
    return <Icons.Git className={iconClass}/>
  }
}

const generateJavaDocUrl = (gav: Gav) => {
  return `https://javadoc.io/doc/${gav.groupId}/${gav.artifactId}/${gav.version}`
}