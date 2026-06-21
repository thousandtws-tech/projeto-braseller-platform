'use client'

import Link from 'next/link'
import { useActionState } from 'react'
import { AlertCircle, CheckCircle2, Loader2, Mail } from 'lucide-react'
import { forgotPasswordAction } from '@/features/auth/server/actions'
import { Button } from '@/shared/ui/button'
import { GlassInputWrapper } from '@/shared/ui/glass-input-wrapper'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import type { Locale } from '@/shared/i18n/config'

interface Props {
  lang: Locale
}

export function ForgotPasswordForm({ lang }: Props) {
  const [state, formAction, isPending] = useActionState(forgotPasswordAction, null)
  const resetHref = state?.email
    ? `/${lang}/reset-password?email=${encodeURIComponent(state.email)}`
    : `/${lang}/reset-password`

  return (
    <form action={formAction} className="space-y-5">
      {state?.error && (
        <div className="animate-element flex items-start gap-2.5 rounded-lg border border-destructive/30 bg-destructive/8 px-3.5 py-3 text-sm text-destructive">
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>{state.error}</span>
        </div>
      )}

      {state?.success && (
        <div className="animate-element flex items-start gap-2.5 rounded-lg border border-emerald-500/30 bg-emerald-500/8 px-3.5 py-3 text-sm text-emerald-700 dark:text-emerald-400">
          <CheckCircle2 className="mt-0.5 size-4 shrink-0" />
          <span>{state.success}</span>
        </div>
      )}

      <div className="animate-element animate-delay-100 space-y-1.5">
        <Label htmlFor="email">E-mail</Label>
        <GlassInputWrapper>
          <div className="relative">
            <Mail className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              placeholder="seu@email.com"
              required
              disabled={isPending}
              defaultValue={state?.email ?? ''}
              className="h-auto rounded-2xl border-0 bg-transparent px-4 py-3 pl-11 text-sm focus-visible:ring-0"
            />
          </div>
        </GlassInputWrapper>
      </div>

      <Button type="submit" className="animate-element animate-delay-200 h-auto w-full rounded-2xl py-3" disabled={isPending}>
        {isPending ? (
          <>
            <Loader2 className="size-4 animate-spin" />
            Enviando...
          </>
        ) : (
          'Enviar codigo'
        )}
      </Button>

      <div className="animate-element animate-delay-300 flex items-center justify-between text-sm">
        <Link href={`/${lang}/login`} className="text-muted-foreground transition-colors hover:text-primary">
          Voltar ao login
        </Link>
        <Link href={resetHref} className="font-medium text-primary hover:underline">
          Ja tenho um codigo
        </Link>
      </div>
    </form>
  )
}
