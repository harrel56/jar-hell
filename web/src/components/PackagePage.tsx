import {VersionPicker} from '@/components/VersionPicker.tsx'
import {Outlet, useLoaderData} from 'react-router-dom'
import {PackageLoaderData} from '@/router.tsx'
import {Separator} from '@/shadcn/components/ui/Separator.tsx'
import {Package, ResolvedPackage} from '@/util.ts'
import {useCallback, useRef, useState} from 'react'

export interface OutletContext {
  versions: string[]
  analyzedPackages: Package[]
  markAsAnalyzed: (pkg: Package) => void
}

export const PackagePage = () => {
  const loaderData = useLoaderData() as PackageLoaderData
  const [analyzedPackages, setAnalyzedPackages] = useState(loaderData.analyzedPackages)
  const prevLoaderDataRef = useRef(loaderData)

  if (prevLoaderDataRef.current !== loaderData) {
    setAnalyzedPackages(loaderData.analyzedPackages)
    prevLoaderDataRef.current = loaderData
  }
  const markAsAnalyzed = useCallback((pkg: ResolvedPackage) => {
    loaderData.analyzedPackages.push(pkg)
    setAnalyzedPackages(pkgs => [...pkgs, pkg])
  }, [loaderData.analyzedPackages])

  return (
    <div className='flex basis-1 gap-4'>
      <VersionPicker versions={loaderData.versions} analyzedPackages={analyzedPackages}/>
      <Separator orientation='vertical' className='h-auto'/>
      <div className='min-w-[600px] min-h-[400px] w-full flex justify-center px-4'>
        <Outlet context={{versions: loaderData.versions, analyzedPackages, markAsAnalyzed}}/>
      </div>
    </div>)
}