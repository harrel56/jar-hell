import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/shadcn/components/ui/card'

export const Footer = () => {
  return (
    <div className='grow max-h-[800px] min-h-[800px] bg-gradient-to-b from-background to-lava-ambient p-12'>
      <div className='max-w-[1400px] m-auto'>
        <div className='flex gap-8'>
          <h2
            className='[writing-mode:sideways-lr] text-center text-2xl font-semibold leading-none tracking-tight text-fade'>Recently
            analyzed</h2>
          <Card className='min-w-[200px] max-w-[360px]'>
            <CardHeader>
              <CardDescription>dev.harrel</CardDescription>
              <CardTitle className='mt-0'>json-schema</CardTitle>
              <CardDescription>1.7.3</CardDescription>
            </CardHeader>
            <CardContent>
              <div className='grid grid-rows-2 grid-cols-[35%_65%] gap-[1px] bg-border'>
                <div className='p-2 bg-background text-center truncate'>189.93KB</div>
                <div className='p-2 bg-background text-center truncate'>Java 8</div>
                <div className='p-2 bg-background text-center truncate'>0 deps</div>
                <div className='p-2 bg-background text-center truncate'>The Lesser GNU License</div>
              </div>
            </CardContent>
          </Card>
          <Card className='min-w-[200px] max-w-[360px]'>
            <CardHeader>
              <CardDescription>dev.harrel</CardDescription>
              <CardTitle className='mt-0'>json-schema</CardTitle>
              <CardDescription>1.7.3</CardDescription>
            </CardHeader>
            <CardContent>
              <div className='grid grid-rows-2 grid-cols-[35%_65%] gap-[1px] bg-border'>
                  <div className='p-2 bg-background text-center truncate'>189.93KB</div>
                  <div className='p-2 bg-background text-center truncate'>Java 8</div>
                  <div className='p-2 bg-background text-center truncate'>0 deps</div>
                  <div className='p-2 bg-background text-center truncate'>The Lesser GNU License</div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}