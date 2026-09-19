import { createMemo, Errored, Loading } from 'solid-js'
import Autocomplete from '../components/Autocomplete'
import { getAnalyzedCount } from '../api'
import { RecentlyViewed } from '../components/home/RecentlyViewed'
import { RecentlyAnalyzed } from '../components/home/RecentlyAnalyzed'

const formatCount = (count: number) => count.toLocaleString('en-US')

function AnalyzedCount() {
  const count = createMemo(() => getAnalyzedCount())
  return <>{formatCount(count())} packages analysed</>
}

export function HomePage() {
  return (
    <main class="mx-auto w-full max-w-(--measure-index) px-10 pb-[110px]">
      <section class="pt-[78px] text-center">
        <h1 class="mx-auto max-w-[700px] text-(length:--text-h1) font-semibold leading-[1.08] tracking-[-0.035em] text-(--ink)">
          What does that dependency <span class="whitespace-nowrap text-(--accent)">really cost?</span>
        </h1>
        <p class="mx-auto mt-3 max-w-[520px] text-[15.5px] text-(--ink-3)">
          Jarhell resolves a Maven artifact's full transitive graph and reports its weight, bytecode floor and effective license.
        </p>

        <Autocomplete variant="hero" class="mx-auto mt-[30px] max-w-[620px] text-left"/>

        <div class="mt-[22px] font-(family-name:--font-data) text-(length:--text-meta) text-(--ink-5)">
          <Errored fallback={<>&nbsp;</>}>
            <Loading fallback={<>&nbsp;</>}>
              <AnalyzedCount/>
            </Loading>
          </Errored>
        </div>
      </section>

      <div class="mt-[74px]">
        <RecentlyViewed class="mt-[34px]"/>
        <RecentlyAnalyzed class="mt-[34px]"/>
      </div>
    </main>
  )
}
