import {createMemo, For, Show} from 'solid-js'
import { EffectiveValues } from '../../../api'
import { Verdict, verdict, VerdictPill } from './VerdictPill'
import {formatLicenseType, LicenseKind} from '../../../utils/utils'

const LICENSE_VERDICTS: Record<LicenseKind, Verdict> = {
  'permissive': verdict('Permissive', 'good'),
  'weak-copyleft': verdict('Weak copyleft', 'warn'),
  'copyleft': verdict('Copyleft', 'bad'),
  'risky': verdict('Legally unclear', 'critical'),
  'unusable': verdict('Unusable', 'critical'),
  'unknown': verdict('Unknown', 'ink-4'),
}

export function EffectiveLicense(props: { effective: EffectiveValues }) {
  const effective = createMemo(() => formatLicenseType(props.effective.licenseType))
  const others = createMemo(() => props.effective.licenseTypes
    .flatMap(entry => Object.keys(entry))
    .filter(type => type !== props.effective.licenseType)
    .map(lic => formatLicenseType(lic)[0]))
  const distinct = () => props.effective.licenseTypes.length

  return (
    <div class="bg-(--ground) px-6 pt-[22px] pb-5">
      <div class="whitespace-nowrap font-(family-name:--font-data) text-(length:--text-metric) font-medium leading-none tracking-[-0.035em]">
        {effective()[0]}
      </div>
      <div class="mt-3 flex items-center gap-2.5">
        <span class="text-[14px] font-semibold">Effective license</span>
        <VerdictPill verdict={LICENSE_VERDICTS[effective()[1]]}/>
      </div>
      <Show when={others().length}>
        <div class="mt-4 flex min-h-[26px] flex-wrap items-center gap-1.5">
          <For each={others()}>
            {license => (
              <span class="rounded-(--radius-chip) bg-(--chip-bg) px-[9px] py-1 font-(family-name:--font-data) text-[12px] text-(--ink)">
                {license}
              </span>
            )}
          </For>
        </div>
      </Show>
      <div class="mt-2.5 text-(length:--text-meta) text-(--ink-4)">
        {distinct() === 1 ? '1 distinct license.' : `${distinct()} distinct licenses.`}
      </div>
    </div>
  )
}
