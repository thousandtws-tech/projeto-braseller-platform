'use client'

import { useActionState, useEffect } from 'react'
import { AlertCircle, CheckCircle2, Loader2, Mail, RotateCw, ShieldCheck } from 'lucide-react'
import { resendEmailVerificationCodeAction, verifyEmailCodeAction } from '@/features/auth/server/actions'
import { appToast } from '@/shared/lib/toast'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'

type VerifyCodeFormProps = {
  email: string
  registered?: boolean
}

export function VerifyCodeForm({ email, registered = false }: VerifyCodeFormProps) {
  const [verifyState, verifyAction, isVerifying] = useActionState(verifyEmailCodeAction, null)
  const [resendState, resendAction, isResending] = useActionState(resendEmailVerificationCodeAction, null)

  useEffect(() => {
    if (registered) {
      appToast.success('Enviamos um código de verificação para o e-mail informado no cadastro.')
    }
  }, [registered])

  useEffect(() => {
    if (verifyState?.error) {
      appToast.error(verifyState.error)
    }
  }, [verifyState])

  useEffect(() => {
    if (resendState?.success) {
      appToast.success(resendState.success)
    }

    if (resendState?.error) {
      appToast.error(resendState.error)
    }
  }, [resendState])

  return (
    <div className="flex flex-col gap-5">
      {verifyState?.error ? (
        <div className="flex items-start gap-2.5 rounded-md border border-destructive/25 bg-destructive/5 px-3.5 py-3 text-sm text-destructive">
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>{verifyState.error}</span>
        </div>
      ) : null}

      {resendState?.success ? (
        <div className="flex items-start gap-2.5 rounded-md border border-emerald-500/25 bg-emerald-500/5 px-3.5 py-3 text-sm text-emerald-700">
          <CheckCircle2 className="mt-0.5 size-4 shrink-0" />
          <span>{resendState.success}</span>
        </div>
      ) : null}

      {resendState?.error ? (
        <div className="flex items-start gap-2.5 rounded-md border border-destructive/25 bg-destructive/5 px-3.5 py-3 text-sm text-destructive">
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>{resendState.error}</span>
        </div>
      ) : null}

      <div className="rounded-xl border border-border bg-muted/40 p-4">
        <div className="flex items-center gap-2 text-sm font-medium text-foreground">
          <Mail className="size-4" />
          <span>{email}</span>
        </div>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          Digite o código de verificação enviado para este e-mail para liberar o acesso ao Brasaller.
        </p>
      </div>

      <form action={verifyAction} className="flex flex-col gap-5">
        <input type="hidden" name="email" value={email} />

        <div className="flex flex-col gap-2">
          <Label htmlFor="code">Código de verificação</Label>
          <Input
            id="code"
            name="code"
            inputMode="numeric"
            autoComplete="one-time-code"
            placeholder="000000"
            maxLength={6}
            pattern="[0-9]{6}"
            required
            disabled={isVerifying}
            className="h-12 text-center text-lg tracking-[0.45em]"
          />
          <p className="text-xs text-muted-foreground">Use o código de 6 dígitos enviado para o seu e-mail.</p>
        </div>

        <div className="rounded-xl border border-border bg-background p-4">
          <div className="flex items-start gap-3 text-sm text-muted-foreground">
            <ShieldCheck className="mt-0.5 size-4 shrink-0 text-foreground" />
            <p className="leading-6">
              Sua conta permanece bloqueada até a validação do e-mail. Nenhum token de sessão é criado antes dessa etapa.
            </p>
          </div>
        </div>

        <Button type="submit" size="lg" className="w-full" disabled={isVerifying}>
          {isVerifying ? <><Loader2 className="animate-spin" />Validando código...</> : 'Validar e continuar'}
        </Button>
      </form>

      <form action={resendAction}>
        <input type="hidden" name="email" value={email} />
        <Button type="submit" variant="outline" className="w-full" disabled={isResending}>
          {isResending ? <><Loader2 className="animate-spin" />Reenviando código...</> : <><RotateCw />Reenviar código</>}
        </Button>
      </form>
    </div>
  )
}
