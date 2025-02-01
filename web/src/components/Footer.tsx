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

export const Footer = () => {
  const { recentlyViewed } = use(RecentlyViewedContext)
  const { data, get } = useFetch<ResolvedPackage[]>('/api/v1/packages/latest')
  useEffect(() => void(get('')), [])

  if (!data) {
    return
  }
  return (
    <div className='grow max-h-[800px] min-h-[800px] bg-gradient-to-b from-background to-lava-ambient py-12 px-4'>
      <div className='max-w-[1400px] m-auto'>
        {Boolean(recentlyViewed.length) &&
        <div className='flex gap-16 mr-16'>
          <h2 className='[writing-mode:sideways-lr] text-center text-2xl font-semibold leading-none tracking-tight text-fade'>
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
            <CarouselPrevious />
            <CarouselNext />
          </Carousel>
        </div>}
        <div className='flex gap-16 mr-16'>
          <h2 className='[writing-mode:sideways-lr] text-center text-2xl font-semibold leading-none tracking-tight text-fade'>
            Recently analyzed
          </h2>
          <Carousel opts={({dragFree: true})} className='min-w-0'>
            <CarouselContent className='py-2'>
              {data.map(pkg =>
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