import {useOutletContext, useParams} from 'react-router-dom'
import {useFetch} from '@/hooks/useFetch.ts'
import {ResolvedPackage, stringToGav, formatBytes, formatBytecodeVersion} from '@/util.ts'
import {useLayoutEffect, useMemo} from 'react'
import {MetricDisplay} from '@/components/MetricDisplay.tsx'
import {PendingAnalysis} from '@/components/PendingAnalysis.tsx'
import {LoadingSpinner} from '@/components/LoadingSpinner.tsx'
import {OutletContext} from '@/components/PackagePage.tsx'
import {Alert, AlertDescription, AlertTitle} from '@/shadcn/components/ui/Alert.tsx'
import {AlertCircle} from 'lucide-react'

export const ArtifactInfo = () => {
  const ctx = useOutletContext<OutletContext>()
  const { gav } = useParams()
  const gavObject = useMemo(() => stringToGav(gav!), [gav])
  const {data: getData, loading: getLoading, error: getError, get} = useFetch<ResolvedPackage>(`/api/v1/packages/${gav}?depth=1`, [gav])
  const {data: postData, loading: postLoading, error: postError, post} = useFetch<ResolvedPackage>('/api/v1/analyze-and-wait', [gav])
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
    return <p>This package couldn't be analyzed</p>
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
  const packageSize = {
    title: 'Package size',
    value: formatBytes(packageData.packageSize),
    valueHint: packageData.packageSize + ' bytes'
  }
  const requiredDependencies = {
    title: 'All required dependencies',
    value: packageData.effectiveValues.requiredDependencies,
  }
  const optionalDependencies = {
    title: 'All optional dependencies',
    value: packageData.effectiveValues.optionalDependencies,
  }
  return (
    <div>
      {packageData.effectiveValues.unresolvedDependencies > 0 && (
        <Alert variant='destructive' className='mb-6'>
          <AlertCircle className='h-4 w-4'/>
          <AlertTitle>Analysis was not fully completed</AlertTitle>
          <AlertDescription>
            A total of <strong>{packageData.effectiveValues.unresolvedDependencies}</strong> required dependencies were not resolved.
            This may happen if the package is not available in the Maven Central repository or due to intermittent server/network issues.
            All effective values should be treated only as a rough estimations.
          </AlertDescription>
        </Alert>
      )}
      <div className='grid xl:grid-cols-6 lg:grid-cols-4 grid-cols-2 gap-x-32 gap-y-16 h-fit'>
        <MetricDisplay className='xl:col-start-2 col-span-2' {...effectiveSize}/>
        <MetricDisplay className='col-span-2' {...effectiveBytecodeVersion}/>
        <MetricDisplay className='col-span-2' {...packageSize}/>
        <MetricDisplay className='col-span-2' {...requiredDependencies}/>
        <MetricDisplay className='col-span-2' {...optionalDependencies}/>
      </div>
    </div>
  )
}