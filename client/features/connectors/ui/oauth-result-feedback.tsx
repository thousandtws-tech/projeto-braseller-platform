'use client'

import { useEffect } from 'react'
import { usePathname, useRouter } from 'next/navigation'
import { AlertCircle, CheckCircle2 } from 'lucide-react'

interface Props {
  successMessage?: string
  errorMessage?: string
}

export function OAuthResultFeedback({ successMessage, errorMessage }: Props) {
  const pathname = usePathname()
  const router = useRouter()

  useEffect(() => {
    if (!successMessage && !errorMessage) return

    router.refresh()
    const timeout = window.setTimeout(() => {
      router.replace(pathname, { scroll: false })
    }, 4000)

    return () => window.clearTimeout(timeout)
  }, [errorMessage, pathname, router, successMessage])

  if (successMessage) {
    return <Feedback icon={CheckCircle2} text={successMessage} />
  }

  if (errorMessage) {
    return <Feedback icon={AlertCircle} text={errorMessage} destructive />
  }

  return null
}

function Feedback({
  icon: Icon,
  text,
  destructive = false,
}: {
  icon: React.ComponentType<{ className?: string }>
  text: string
  destructive?: boolean
}) {
  return (
    <div
      className={
        destructive
          ? 'flex items-center gap-3 rounded-md border border-destructive/25 bg-destructive/5 p-4 text-sm text-destructive'
          : 'flex items-center gap-3 rounded-md border border-border bg-muted/40 p-4 text-sm'
      }
      role={destructive ? 'alert' : 'status'}
    >
      <Icon className="size-4 shrink-0" />
      <p className="font-medium">{text}</p>
    </div>
  )
}
