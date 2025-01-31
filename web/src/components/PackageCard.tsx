import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/shadcn/components/ui/Card.tsx'
import {formatBytecodeVersion, formatBytes, formatLicenseType, gavToString, ResolvedPackage} from '@/util.ts'
import {Link} from 'react-router-dom'

export const PackageCard = ({pkg}: { pkg: ResolvedPackage }) => {
  return (
    <Link to={`/packages/${gavToString(pkg)}`}>
      <Card
        className='min-w-[300px] max-w-[300px] cursor-pointer transition-all hover:brightness-[98%] dark:hover:brightness-[130%] hover:-translate-y-2'>
        <CardHeader>
          <CardDescription>{pkg.groupId}</CardDescription>
          <CardTitle>{pkg.artifactId}</CardTitle>
          <CardDescription>{pkg.version}</CardDescription>
        </CardHeader>
        <CardContent>
          <div className='grid grid-rows-2 grid-cols-[35%_65%] gap-[1px] bg-border'>
            <div className='p-2 bg-background text-center truncate'>
              {formatBytes(pkg.effectiveValues.size)}
            </div>
            <div className='p-2 bg-background text-center truncate'>
              {pkg.effectiveValues.requiredDependencies + ' dependencies'}
            </div>
            <div className='p-2 bg-background text-center truncate'>
              {formatBytecodeVersion(pkg.effectiveValues.bytecodeVersion)}
            </div>
            <div className='p-2 bg-background text-center truncate'>
              {formatLicenseType(pkg.effectiveValues.licenseType)}
            </div>
          </div>
        </CardContent>
      </Card>
    </Link>
  )
}