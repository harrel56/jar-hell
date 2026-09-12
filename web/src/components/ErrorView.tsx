import { Show } from 'solid-js'
import { HttpError } from '../api'
import {useParams} from '@solidjs/router'

export interface ErrorViewProps {
  code: number | string
  title: string
  details: string
}

export function ErrorView(props: ErrorViewProps) {
  const params = useParams()
  return (
    <main class="flex min-h-[calc(100vh-var(--header-height))] min-w-0 flex-1 flex-col items-center justify-center px-10 py-(--space-10)">
      <div class="max-w-[520px] pb-[8vh] text-center">
        <div class="font-(family-name:--font-data) text-[92px] font-bold leading-(--leading-metric) tracking-[-0.05em] text-(--accent)">
          {props.code}
        </div>
        <h1 class="mt-5 text-[32px] font-semibold leading-[1.15] tracking-(--tracking-tighter) text-(--ink)">
          {props.title}
        </h1>
        <p class="mt-3.5 text-(length:--text-lead) text-(--ink-2)">{props.details}</p>

        <Show when={params['coordinate']}>
          <div class="mt-5.5 inline-flex max-w-full items-center gap-3 rounded-(--radius-nav) border border-(--hairline) bg-(--surface) px-4 py-3">
            <span class="font-semibold uppercase tracking-[0.06em] text-(length:--text-label) text-(--ink-5)">
              Requested
            </span>
            <span class="break-all font-(family-name:--font-data) text-(length:--text-body) text-(--ink)">
              {params['coordinate']}
            </span>
          </div>
        </Show>

        <div class="mt-7 flex flex-wrap justify-center gap-2 text-(length:--text-sm)">
          <a href="/" class="flex items-center gap-1.75 rounded-(--radius-button) bg-(--ink) px-3.5 py-2 text-(--ground) hover:bg-(--ink-2) hover:text-(--ground)">
            Back to home
          </a>
        </div>
      </div>
    </main>
  )
}

export function NotFoundView() {
  return (
    <ErrorView
      code={404}
      title="Package not found"
      details="This coordinate was not found in the Maven Central repository."
    />
  )
}

export function ThrownErrorView(props: { error: unknown }) {
  return (
    <Show when={props.error instanceof HttpError && props.error.status === 404}
          fallback={<ErrorView {...describeError(props.error)}/>}>
      <NotFoundView/>
    </Show>
  )
}

const describeError = (error: unknown): ErrorViewProps => {
  if (error instanceof HttpError) {
    return {
      code: error.status,
      title: error.status >= 500 ? 'Server error' : 'Request failed',
      details: error.message,
    }
  }
  return {
    code: 'Oops',
    title: 'Something went wrong',
    details: error instanceof Error ? error.message : String(error),
  }
}
