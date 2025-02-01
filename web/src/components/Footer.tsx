import {PackageCard} from '@/components/PackageCard.tsx'
import {gavToString, ResolvedPackage} from '@/util.ts'
import {
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious
} from '@/shadcn/components/ui/Carousel.tsx'
import {useFetch} from '@/hooks/useFetch.ts'
import {use, useEffect} from 'react'
import {RecentlyViewedContext} from '@/context/RecentlyViewedProvider.tsx'
import {Separator} from '@/shadcn/components/ui/Separator.tsx'

const numberFormat = new Intl.NumberFormat()

export const Footer = () => {
  const {recentlyViewed} = use(RecentlyViewedContext)
  const {data: packagesCount, get: getPackagesCount} = useFetch<number>('/api/v1/packages/count')
  const {data: latestPackages, get: getLatestPackages} = useFetch<ResolvedPackage[]>('/api/v1/packages/latest')
  useEffect(() => {
    getPackagesCount('').finally()
    getLatestPackages('').finally()
  }, [])

  if (!packagesCount || !latestPackages) {
    return
  }
  return (
    <div className='grow bg-gradient-to-b from-background to-lava-ambient pb-6 px-4 pt-12'>
      <div className='max-w-[1400px] m-auto flex flex-col gap-12'>
        <Separator></Separator>
        {Boolean(packagesCount) && <div className='flex flex-col items-center gap-2'>
          <h2 className='text-6xl font-semibold leading-none tracking-tight text-fade font-mono'>
            {numberFormat.format(packagesCount!)}
          </h2>
          <span className='font-semibold'>
            analyzed packages
          </span>
        </div>}
        {Boolean(recentlyViewed.length) &&
          <div className='flex flex-col gap-2 mx-12 lg:mx-[4.5rem]'>
            <h2 className='text-2xl font-semibold leading-none tracking-tight text-fade'>
              Recently viewed
            </h2>
            <Carousel opts={({dragFree: true})} className='min-w-0'>
              <CarouselContent className='py-2'>
                {recentlyViewed.map(pkg =>
                  <CarouselItem key={gavToString(pkg)} className='basis-auto'>
                    <PackageCard pkg={pkg}/>
                  </CarouselItem>
                )}
              </CarouselContent>
              <CarouselPrevious/>
              <CarouselNext/>
            </Carousel>
          </div>}
        {latestPackages &&
          <div className='flex flex-col gap-2 mx-12 lg:mx-[4.5rem]'>
            <h2 className='text-2xl font-semibold leading-none tracking-tight text-fade'>
              Recently analyzed
            </h2>
            <Carousel opts={({dragFree: true})} className='min-w-0'>
              <CarouselContent className='py-2'>
                {latestPackages.map(pkg =>
                  <CarouselItem key={gavToString(pkg)} className='basis-auto'>
                    <PackageCard pkg={pkg}/>
                  </CarouselItem>
                )}
              </CarouselContent>
              <CarouselPrevious/>
              <CarouselNext/>
            </Carousel>
          </div>}
      </div>
    </div>
  )
}