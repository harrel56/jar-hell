import { createMemo, Errored, Loading, Show } from 'solid-js'
import { getLatestAnalyzed } from '../../api'
import { toPackageRow } from '../../utils/packageRow'
import { PackageRows, PackageRowsSkeleton } from './PackageRows'

const TITLE = 'Recently analyzed'
const META = 'across all users'
/** matches the server-side cap on `/packages/latest`, only used to size the skeleton */
const SKELETON_ROWS = 6

export function RecentlyAnalyzed(props: { class?: string }) {
  const rows = createMemo(async () => (await getLatestAnalyzed()).map(toPackageRow))
  return (
    <Errored fallback={null}>
      <Loading fallback={<PackageRowsSkeleton title={TITLE} meta={META} count={SKELETON_ROWS} class={props.class}/>}>
        <Show when={rows().length > 0}>
          <PackageRows title={TITLE} meta={META} rows={rows()} class={props.class}/>
        </Show>
      </Loading>
    </Errored>
  )
}
