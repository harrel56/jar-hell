import {Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle} from '@/shadcn/components/ui/card'

export const Footer = () => {
  return (
    <div className='grow max-h-[800px] min-h-[800px] bg-gradient-to-b from-background to-lava-ambient p-12'>
      <div className='max-w-[1400px] m-auto'>
        <div className='flex gap-8'>
          <h2
            className='[writing-mode:sideways-lr] text-center text-2xl font-semibold leading-none tracking-tight text-fade'>Recently
            analyzed</h2>
          <Card className=''>
            <CardHeader>
              <CardTitle>Card Title</CardTitle>
              <CardDescription>Card Description</CardDescription>
            </CardHeader>
            <CardContent>
              <p>Card Content</p>
            </CardContent>
            <CardFooter>
              <p>Card Footer</p>
            </CardFooter>
          </Card>
          <Card className=''>
            <CardHeader>
              <CardTitle>Card Title</CardTitle>
              <CardDescription>Card Description</CardDescription>
            </CardHeader>
            <CardContent>
              <p>Card Content</p>
            </CardContent>
            <CardFooter>
              <p>Card Footer</p>
            </CardFooter>
          </Card>
        </div>
      </div>
    </div>
  )
}