import { Show } from 'solid-js'
import {type BytecodeVersion} from '../../../api'
import {formatBytecodeVersion} from '../../../utils/utils'

export function BytecodeVersionCell(props: { bytecodeVersion: BytecodeVersion | undefined }) {
  return (
    <div class="bg-(--ground) px-6 pt-[22px] pb-5">
      <div class="whitespace-nowrap font-(family-name:--font-data) text-(length:--text-metric) font-medium leading-none tracking-[-0.035em]">
        <Show when={props.bytecodeVersion} fallback="N/A">
          {bv => <>Java {formatBytecodeVersion(bv())}</>}
        </Show>
      </div>
      <div class="mt-3 text-[14px] font-semibold">Effective bytecode version</div>
      <div class="mt-4 text-(length:--text-meta) text-(--ink-3)">
        <Show when={props.bytecodeVersion} fallback="No class files were found in the package and its required dependencies.">
          {bv => <>{bv().major <= 52
            ? `Runs on every current LTS. Class file ${bv().major}.`
            : `Requires Java ${formatBytecodeVersion(bv())} or newer. Class file ${bv().major}.`}</>}
        </Show>
      </div>
    </div>
  )
}
