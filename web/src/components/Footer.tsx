import {PackageCard} from '@/components/PackageCard.tsx'
import {gavToString, ResolvedPackage} from '@/util.ts'
import {
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious
} from '@/shadcn/components/ui/Carousel.tsx'

export const Footer = () => {
  const packages: ResolvedPackage[] = [
    {
      groupId: 'dev.harrel',
      artifactId: 'json-schema',
      version: '1.7.0',
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
      version: '1.7.1',
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
      version: '1.7.2',
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
      version: '1.7.4',
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
      version: '1.7.5',
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
      version: '1.7.6',
      effectiveValues: {
        size: 189_587,
        bytecodeVersion: '52.0',
        requiredDependencies: 0,
        licenseType: 'MIT'
      }
    } as any
  ]
  return (
    <div className='grow max-h-[800px] min-h-[800px] bg-gradient-to-b from-background to-lava-ambient py-12 px-4'>
      <div className='max-w-[1400px] m-auto'>
        <div className='flex gap-16 mr-16'>
          <h2 className='[writing-mode:sideways-lr] text-center text-2xl font-semibold leading-none tracking-tight text-fade'>
            Recently analyzed
          </h2>
          <Carousel opts={({dragFree: true})} className='min-w-0'>
            <CarouselContent className='py-2'>
              {packages.map(pkg =>
                <CarouselItem key={gavToString(pkg)} className='basis-auto'>
                  <PackageCard pkg={pkg}/>
                </CarouselItem>
              )}
            </CarouselContent>
            <CarouselPrevious />
            <CarouselNext />
          </Carousel>
        </div>
      </div>
    </div>
  )
}