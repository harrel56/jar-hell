import { Show } from 'solid-js'
import { recentlyViewed } from '../../utils/recentlyViewed'
import { PackageRows } from './PackageRows'

export function RecentlyViewed(props: { class?: string }) {
  return (
    <Show when={recentlyViewed().length > 0}>
      <PackageRows title="Recently viewed" meta="on this machine" rows={recentlyViewed()} class={props.class}/>
    </Show>
  )
}
