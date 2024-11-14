import {PropsWithChildren} from 'react'
import {Button} from '@/shadcn/components/ui/Button.tsx'
import {Gav, ResolvedPackage} from '@/util.ts'
import {BugIcon, FileCheck, HouseIcon} from 'lucide-react'
import {clsx} from 'clsx'
import {Icons} from '@/shadcn/components/ui/Icons.tsx'

export const PackageLinks = ({pkg}: {pkg: ResolvedPackage}) => {
  return (
    <div className='flex gap-1'>
      <ExternalLink name='Home page' href={pkg.url}>
        <HouseIcon/>
      </ExternalLink>
      <ExternalLink name='Source code management page' href={pkg.scmUrl}>
        {getScmIcon(pkg.scmUrl)}
      </ExternalLink>
      <ExternalLink name='Issue management page' href={pkg.issuesUrl}>
        <BugIcon/>
      </ExternalLink>
      <ExternalLink name='Javadoc page' href={pkg.classifiers.includes('javadoc') ? generateJavaDocUrl(pkg) : undefined}>
        <FileCheck/>
      </ExternalLink>
    </div>
  )
}

const ExternalLink = ({name, href, children}: PropsWithChildren<{ name: string, href?: string }>) => {
  return (
    <a href={href} target='_blank'
       rel='noreferrer'
       title={href ? `${name}` : `${name} not available`}>
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