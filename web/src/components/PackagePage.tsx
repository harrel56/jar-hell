import {VersionPicker} from '@/components/VersionPicker.tsx'
import {Outlet, useLoaderData} from 'react-router-dom'
import {PackageLoaderData} from '@/router.tsx'
import {Separator} from '@/shadcn/components/ui/Separator.tsx'
import {gavEquals, Package, ResolvedPackage} from '@/util.ts'
import {useCallback, useRef, useState} from 'react'

export interface OutletContext {
  versions: string[]
  analyzedPackages: Package[]
  markAsAnalyzed: (pkg: Package) => void
}

export const PackagePage = () => {
  const loaderData = useLoaderData() as PackageLoaderData
  const [analyzedPackages, setAnalyzedPackages] = useState(loaderData.packages.filter(pkg => !pkg.unresolved))
  const prevLoaderDataRef = useRef(loaderData)

  if (prevLoaderDataRef.current !== loaderData) {
    setAnalyzedPackages(loaderData.packages.filter(pkg => !pkg.unresolved))
    prevLoaderDataRef.current = loaderData
  }
  const markAsAnalyzed = useCallback((pkg: ResolvedPackage) => {
    const idx = loaderData.packages.findIndex(pkg2 => gavEquals(pkg, pkg2))
    loaderData.packages[idx] = pkg
    setAnalyzedPackages(pkgs => [...pkgs, pkg])
  }, [loaderData.packages])
  const versions = loaderData.packages.map(pkg => pkg.version).toReversed()

  return (
    <div className='max-w-[1400px] w-full self-center pt-12'>
      <div className='flex flex-col md:flex-row basis-1 gap-4'>
        <VersionPicker versions={versions} analyzedPackages={analyzedPackages}/>
        <Separator orientation='vertical' className='h-auto'/>
        <div className='min-h-[400px] w-full flex justify-center px-4'>
          <Outlet context={{versions: versions, analyzedPackages, markAsAnalyzed}}/>
        </div>
      </div>
    </div>)
}