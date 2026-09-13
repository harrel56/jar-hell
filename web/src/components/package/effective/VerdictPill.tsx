export interface Verdict {
  label: string
  color: string
}

export const verdict = (label: string, token: string): Verdict => ({ label, color: `var(--${token})` })

export function VerdictPill(props: { verdict: Verdict }) {
  return (
    <span class="rounded-(--radius-pill) px-2 py-[3px] text-(length:--text-label) font-semibold uppercase tracking-[0.05em] text-white"
          style={{ background: props.verdict.color }}>
      {props.verdict.label}
    </span>
  )
}
