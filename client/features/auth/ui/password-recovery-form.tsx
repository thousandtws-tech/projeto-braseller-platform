'use client'

import { useActionState } from 'react'
import { CheckCircle2, Loader2, Mail } from 'lucide-react'
import { requestPasswordRecoveryAction } from '@/features/auth/server/actions'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'

export function PasswordRecoveryForm() {
  const [state, formAction, isPending] = useActionState(requestPasswordRecoveryAction, null)

  if (state?.success) {
    return (
      <div className="flex flex-col gap-4 rounded-md border border-border bg-muted/45 p-5">
        <CheckCircle2 className="size-5" />
        <div>
          <p className="font-medium">Verifique seu e-mail</p>
          <p className="mt-1 text-sm leading-6 text-muted-foreground">
            Se a conta existir, você receberá as instruções para criar uma nova senha.
          </p>
        </div>
      </div>
    )
  }

  return (
    <form action={formAction} className="flex flex-col gap-5">
      {state?.error ? (
        <p className="rounded-md border border-destructive/25 bg-destructive/5 px-3.5 py-3 text-sm text-destructive">
          {state.error}
        </p>
      ) : null}
      <div className="flex flex-col gap-2">
        <Label htmlFor="email">E-mail da conta</Label>
        <Input id="email" name="email" type="email" autoComplete="email" placeholder="seu@email.com" required disabled={isPending} />
      </div>
      <Button type="submit" size="lg" disabled={isPending}>
        {isPending ? <><Loader2 className="animate-spin" />Enviando...</> : <><Mail />Enviar instruções</>}
      </Button>
    </form>
  )
}
