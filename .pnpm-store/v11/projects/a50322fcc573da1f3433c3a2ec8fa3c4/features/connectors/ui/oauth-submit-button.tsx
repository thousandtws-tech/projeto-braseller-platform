'use client'

import { useFormStatus } from 'react-dom'
import { ExternalLink, Loader2 } from 'lucide-react'

import { Button } from '@/shared/ui/button'

interface Props {
  idleLabel: string
  pendingLabel: string
}

export function OAuthSubmitButton({ idleLabel, pendingLabel }: Props) {
  const { pending } = useFormStatus()

  return (
    <Button type="submit" size="lg" disabled={pending}>
      {pending ? (
        <>
          <Loader2 className="size-4 animate-spin" />
          {pendingLabel}
        </>
      ) : (
        <>
          {idleLabel}
          <ExternalLink className="size-4" />
        </>
      )}
    </Button>
  )
}
