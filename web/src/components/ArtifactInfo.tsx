import {useOutletContext, useParams} from 'react-router-dom'
import {useFetch} from '@/hooks/useFetch.ts'
import {
  stringToGav,
  formatBytes,
  formatBytecodeVersion,
  Package,
  formatDate,
  formatDateTime,
  formatLicenseType, formatPackageLicenseType, formatLicenseTypesMap
} from '@/util.ts'
import {useLayoutEffect, useMemo} from 'react'
import {MetricDisplay} from '@/components/MetricDisplay.tsx'
import {PendingAnalysis} from '@/components/PendingAnalysis.tsx'
import {LoadingSpinner} from '@/components/LoadingSpinner.tsx'
import {OutletContext} from '@/components/PackagePage.tsx'
import {Alert, AlertDescription, AlertTitle} from '@/shadcn/components/ui/Alert.tsx'
import {AlertCircle, Info} from 'lucide-react'
import {Separator} from '@/shadcn/components/ui/Separator.tsx'
import {ArtifactTree} from '@/components/ArtifactTree.tsx'
import {PackageLinks} from '@/components/PackageLinks.tsx'

export const ArtifactInfo = () => {
  const ctx = useOutletContext<OutletContext>()
  const { gav } = useParams()
  const gavObject = useMemo(() => stringToGav(gav!), [gav])
  const {data: getData, loading: getLoading, error: getError, get} = useFetch<Package>(`/api/v1/packages/${gav}?depth=1`, [gav])
  const {data: postData, loading: postLoading, error: postError, post} = useFetch<Package>('/api/v1/analyze-and-wait', [gav])
  const packageData = getData ?? postData
  const error = getError ?? postError

  useLayoutEffect(() => {
    if (ctx.analyzedPackages.some(pkg => pkg.version === gavObject.version)) {
      get(`/api/v1/packages/${gav}?depth=1`)
    } else {
      post('/api/v1/analyze-and-wait', JSON.stringify(gavObject))
    }
  }, [gav])
  useLayoutEffect(() => {
    if (postData) {
      ctx.markAsAnalyzed(postData)
    }
  }, [postData])

  if (getLoading) {
    return <LoadingSpinner className='w-16 h-16 my-6'/>
  }
  if (postLoading) {
    return <PendingAnalysis/>
  }
  if (error) {
    return <p className='text-destructive'>Error occurred: {error.data?.message}</p>
  }
  if (!packageData) {
    return null
  }
  if (packageData.unresolved) {
    return (
      <Alert variant='destructive' className='h-fit'>
        <AlertCircle className='h-4 w-4'/>
        <AlertTitle>This package couldn't be analyzed and is marked as unresolved</AlertTitle>
        <AlertDescription>
          This may happen if the package is not available in the Maven Central repository or due to intermittent
          server/network issues.
        </AlertDescription>
      </Alert>
    )
  }

  const effectiveSize = {
    title: 'Effective size',
    titleHint:
      `The total (estimated) size of the package with all its required dependencies.
        Optional dependencies are not included.
        The resulting installation cost might be lower if some of the package's dependencies are already installed.`,
    value: formatBytes(packageData.effectiveValues.size),
    valueHint: packageData.effectiveValues.size + ' bytes'
  }
  const effectiveBytecodeVersion = {
    title: 'Effective bytecode version',
    titleHint:
      `The maximum bytecode version of the package and its required dependencies.
        Can be viewed as a minimal Java version required for using this package.
        '*' symbol means that source code was compiled with preview features enabled.`,
    value: formatBytecodeVersion(packageData.effectiveValues.bytecodeVersion),
    valueHint: packageData.effectiveValues.bytecodeVersion
  }
  const effectiveLicenseType = {
    title: 'Effective license',
    titleHint:
      `<p>The most restrictive license being used in dependencies (only required) or the package itself.
        The restrictiveness is arbitrarily assigned for each license type.
        This metric is a great simplification of licensing subject, and it does not take into account many important factors (like license compatibility).
        It should be primarily used to find possibly overlooked unwanted licenses.</p>
        <br>
        
        <p>Number of license types found across dependencies:<br>
        ${formatLicenseTypesMap(packageData.effectiveValues.licenseTypes)}</p>`,
    value: formatLicenseType(packageData.effectiveValues.licenseType)
  }
  const packageSize = {
    title: 'Package size',
    value: formatBytes(packageData.packageSize),
    valueHint: packageData.packageSize + ' bytes'
  }
  const bytecodeVersion = {
    title: 'Bytecode version',
    value: formatBytecodeVersion(packageData.bytecodeVersion),
    valueHint: packageData.bytecodeVersion
  }
  const licenseType = {
    title: 'License',
    value: formatPackageLicenseType(packageData),
    valueHint: packageData.licenses.map(license => `${license.name} (${license.url})`).join('\n')
  }
  const requiredDependencies = {
    title: 'All required dependencies',
    value: packageData.effectiveValues.requiredDependencies,
  }
  const optionalDependencies = {
    title: 'All optional dependencies',
    value: packageData.effectiveValues.optionalDependencies,
  }
  const publicationDate = {
    title: 'Publication date',
    value: formatDate(packageData.created),
    valueHint: formatDateTime(packageData.created)
  }
  const analysisDate = {
    title: 'Analysis date',
    value: formatDate(packageData.analyzed),
    valueHint: formatDateTime(packageData.analyzed)
  }
  return (
    <div className='flex flex-col gap-8 items-center'>
      <span className='text-5xl text-center font-extrabold font-mono underline' title='Package name'>{packageData.name ?? packageData.artifactId}</span>
      {packageData.description && <blockquote className='my-6 text-faded text-center' title='Package desription'>{packageData.description}</blockquote>}
      <PackageLinks pkg={packageData}/>
      <Separator/>
      {packageData.effectiveValues.unresolvedDependencies > 0 && (
        <Alert variant='destructive'>
          <AlertCircle className='h-4 w-4'/>
          <AlertTitle>Analysis was not fully completed</AlertTitle>
          <AlertDescription>
            A total of <strong>{packageData.effectiveValues.unresolvedDependencies}</strong> required dependencies were
            not resolved.
            This may happen if the package is not available in the Maven Central repository or due to intermittent
            server/network issues.
            All effective values should be treated only as a rough estimations.
          </AlertDescription>
        </Alert>
      )}
      <div className='grid xl:grid-cols-6 lg:grid-cols-4 grid-cols-2 gap-x-32 gap-y-16 h-fit'>
        <MetricDisplay className='xl:col-start-2 col-span-2' {...publicationDate}/>
        <MetricDisplay className='col-span-2' {...analysisDate}/>
        <MetricDisplay className='col-span-2' {...effectiveSize}/>
        <MetricDisplay className='col-span-2' {...effectiveBytecodeVersion}/>
        <MetricDisplay className='col-span-2' {...effectiveLicenseType}/>
        <MetricDisplay className='col-span-2' {...packageSize}/>
        <MetricDisplay className='col-span-2' {...bytecodeVersion}/>
        <MetricDisplay className='col-span-2' {...licenseType}/>
        <MetricDisplay className='xl:col-start-2 col-span-2' {...requiredDependencies}/>
        <MetricDisplay className='col-span-2' {...optionalDependencies}/>
      </div>
      <fieldset className='flex flex-col gap-6 rounded-xl border w-full h-full p-8 mb-8 shadow-md'>
        <legend className='text-2xl px-2'>Dependency explorer</legend>
        <Alert>
          <Info className='h-4 w-4'/>
          <AlertTitle>Notice</AlertTitle>
          <AlertDescription>
            Dependency tree might be not completely accurate
            as it does not take into account <strong>excluded packages</strong> and <strong>version conflicts.</strong>
          </AlertDescription>
        </Alert>
        <div className='px-4'>
          <ArtifactTree artifact={packageData}/>
        </div>
      </fieldset>
    </div>
  )
}
