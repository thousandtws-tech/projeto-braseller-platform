'use client'

import Link from 'next/link'
import { useActionState, useState } from 'react'
import { AlertCircle, CheckCircle2, Loader2, MailCheck } from 'lucide-react'
import { requestEmailVerificationAction, verifyEmailAction } from '@/features/auth/server/actions'
import { Button } from '@/shared/ui/button'
import { GlassInputWrapper } from '@/shared/ui/glass-input-wrapper'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import type { Locale } from '@/shared/i18n/config'

interface Props {
  lang: Locale
  email?: string
}

export function VerifyEmailForm({ lang, email }: Props) {
  const [verifyState, verifyAction, isVerifying] = useActionState(verifyEmailAction, null)
  const [resendState, resendAction, isResending] = useActionState(requestEmailVerificationAction, null)
  const [emailValue, setEmailValue] = useState(verifyState?.email ?? resendState?.email ?? email ?? '')

  return (
    <div className="space-y-5">
      <form action={verifyAction} className="space-y-4">
        {verifyState?.error && (
          <div className="animate-element flex items-start gap-2.5 rounded-lg border border-destructive/30 bg-destructive/8 px-3.5 py-3 text-sm text-destructive">
            <AlertCircle className="mt-0.5 size-4 shrink-0" />
            <span>{verifyState.error}</span>
          </div>
        )}

        <div className="animate-element animate-delay-100 space-y-1.5">
          <Label htmlFor="email">E-mail</Label>
          <GlassInputWrapper>
            <div className="relative">
              <MailCheck className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="email"
                name="email"
                type="email"
                autoComplete="email"
                placeholder="seu@email.com"
                required
                disabled={isVerifying}
                value={emailValue}
                onChange={(event) => setEmailValue(event.target.value)}
                className="h-auto rounded-2xl border-0 bg-transparent px-4 py-3 pl-11 text-sm focus-visible:ring-0"
              />
            </div>
          </GlassInputWrapper>
        </div>

        <div className="animate-element animate-delay-200 space-y-1.5">
          <Label htmlFor="code">Codigo de verificacao</Label>
          <GlassInputWrapper>
            <Input
              id="code"
              name="code"
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              placeholder="000000"
              required
              disabled={isVerifying}
              className="h-auto rounded-2xl border-0 bg-transparent px-4 py-3 text-sm tracking-[0.28em] focus-visible:ring-0"
            />
          </GlassInputWrapper>
        </div>

        <Button type="submit" className="animate-element animate-delay-300 h-auto w-full rounded-2xl py-3" disabled={isVerifying}>
          {isVerifying ? (
            <>
              <Loader2 className="size-4 animate-spin" />
              Validando...
            </>
          ) : (
            'Validar e-mail'
          )}
        </Button>
      </form>

      <form action={resendAction} className="animate-element animate-delay-400 space-y-3">
        <input type="hidden" name="email" value={emailValue} />
        {resendState?.error && (
          <div className="flex items-start gap-2.5 rounded-lg border border-destructive/30 bg-destructive/8 px-3.5 py-3 text-sm text-destructive">
            <AlertCircle className="mt-0.5 size-4 shrink-0" />
            <span>{resendState.error}</span>
          </div>
        )}
        {resendState?.success && (
          <div className="flex items-start gap-2.5 rounded-lg border border-emerald-500/30 bg-emerald-500/8 px-3.5 py-3 text-sm text-emerald-700 dark:text-emerald-400">
            <CheckCircle2 className="mt-0.5 size-4 shrink-0" />
            <span>{resendState.success}</span>
          </div>
        )}
        <div className="flex items-center justify-between text-sm">
          <Link href={`/${lang}/login`} className="text-muted-foreground transition-colors hover:text-primary">
            Voltar ao login
          </Link>
          <button
            type="submit"
            disabled={isResending || !emailValue}
            className="font-medium text-primary hover:underline disabled:pointer-events-none disabled:opacity-50"
          >
            {isResending ? 'Reenviando...' : 'Reenviar codigo'}
          </button>
        </div>
      </form>
    </div>
  )
}
