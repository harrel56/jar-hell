import {PackageCard} from '@/components/PackageCard.tsx'
import {ResolvedPackage} from '@/util.ts'

export const Footer = () => {
  const packages: ResolvedPackage[] = [
    {
      groupId: 'dev.harrel',
      artifactId: 'json-schema',
      version: '1.7.3',
      effectiveValues: {
        size: 189_587,
        bytecodeVersion: '52.0',
        requiredDependencies: 0,
        licenseType: 'MIT'
      }
    } as any,
    {
      groupId: 'dev.harrel',
      artifactId: 'json-schema',
      version: '1.7.3',
      effectiveValues: {
        size: 189_587,
        bytecodeVersion: '52.0',
        requiredDependencies: 0,
        licenseType: 'MIT'
      }
    } as any,
    {
      groupId: 'dev.harrel',
      artifactId: 'json-schema',
      version: '1.7.3',
      effectiveValues: {
        size: 189_587,
        bytecodeVersion: '52.0',
        requiredDependencies: 0,
        licenseType: 'MIT'
      }
    } as any,
    {
      groupId: 'dev.harrel',
      artifactId: 'json-schema',
      version: '1.7.3',
      effectiveValues: {
        size: 189_587,
        bytecodeVersion: '52.0',
        requiredDependencies: 0,
        licenseType: 'MIT'
      }
    } as any
  ]
  return (
    <div className='grow max-h-[800px] min-h-[800px] bg-gradient-to-b from-background to-lava-ambient p-12'>
      <div className='max-w-[1400px] m-auto'>
        <div className='flex gap-8'>
          <h2
            className='[writing-mode:sideways-lr] text-center text-2xl font-semibold leading-none tracking-tight text-fade'>Recently
            analyzed</h2>
          {packages.map(pkg =>
            <PackageCard pkg={pkg}/>
          )}
        </div>
      </div>
    </div>
  )
}