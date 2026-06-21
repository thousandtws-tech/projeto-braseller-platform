'use client'

import Link from 'next/link'
import { useActionState, useState } from 'react'
import { AlertCircle, Eye, EyeOff, Loader2 } from 'lucide-react'
import { resetPasswordAction } from '@/features/auth/server/actions'
import { Button } from '@/shared/ui/button'
import { GlassInputWrapper } from '@/shared/ui/glass-input-wrapper'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import type { Locale } from '@/shared/i18n/config'

interface Props {
  lang: Locale
  email?: string
}

export function ResetPasswordForm({ lang, email }: Props) {
  const [state, formAction, isPending] = useActionState(resetPasswordAction, null)
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)

  return (
    <form action={formAction} className="space-y-4">
      {state?.error && (
        <div className="animate-element flex items-start gap-2.5 rounded-lg border border-destructive/30 bg-destructive/8 px-3.5 py-3 text-sm text-destructive">
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>{state.error}</span>
        </div>
      )}

      <div className="animate-element animate-delay-100 space-y-1.5">
        <Label htmlFor="email">E-mail</Label>
        <GlassInputWrapper>
          <Input
            id="email"
            name="email"
            type="email"
            autoComplete="email"
            placeholder="seu@email.com"
            required
            disabled={isPending}
            defaultValue={state?.email ?? email ?? ''}
            className="h-auto rounded-2xl border-0 bg-transparent px-4 py-3 text-sm focus-visible:ring-0"
          />
        </GlassInputWrapper>
      </div>

      <div className="animate-element animate-delay-200 space-y-1.5">
        <Label htmlFor="code">Codigo recebido</Label>
        <GlassInputWrapper>
          <Input
            id="code"
            name="code"
            type="text"
            inputMode="numeric"
            autoComplete="one-time-code"
            placeholder="000000"
            required
            disabled={isPending}
            className="h-auto rounded-2xl border-0 bg-transparent px-4 py-3 text-sm tracking-[0.28em] focus-visible:ring-0"
          />
        </GlassInputWrapper>
      </div>

      <PasswordField
        id="newPassword"
        label="Nova senha"
        autoComplete="new-password"
        show={showPassword}
        onToggle={() => setShowPassword((value) => !value)}
        disabled={isPending}
      />

      <PasswordField
        id="confirmPassword"
        label="Confirmar senha"
        autoComplete="new-password"
        show={showConfirmPassword}
        onToggle={() => setShowConfirmPassword((value) => !value)}
        disabled={isPending}
      />

      <Button type="submit" className="animate-element animate-delay-600 h-auto w-full rounded-2xl py-3" disabled={isPending}>
        {isPending ? (
          <>
            <Loader2 className="size-4 animate-spin" />
            Redefinindo...
          </>
        ) : (
          'Redefinir senha'
        )}
      </Button>

      <p className="animate-element animate-delay-700 text-center text-sm text-muted-foreground">
        <Link href={`/${lang}/forgot-password`} className="font-medium text-primary hover:underline">
          Solicitar novo codigo
        </Link>
      </p>
    </form>
  )
}

interface PasswordFieldProps {
  id: string
  label: string
  autoComplete: string
  show: boolean
  disabled: boolean
  onToggle: () => void
}

function PasswordField({ id, label, autoComplete, show, disabled, onToggle }: PasswordFieldProps) {
  return (
    <div className="animate-element animate-delay-300 space-y-1.5">
      <Label htmlFor={id}>{label}</Label>
      <GlassInputWrapper>
        <div className="relative">
          <Input
            id={id}
            name={id}
            type={show ? 'text' : 'password'}
            autoComplete={autoComplete}
            placeholder="Minimo 8 caracteres"
            minLength={8}
            required
            disabled={disabled}
            className="h-auto rounded-2xl border-0 bg-transparent px-4 py-3 pr-12 text-sm focus-visible:ring-0"
          />
          <button
            type="button"
            tabIndex={-1}
            onClick={onToggle}
            className="absolute inset-y-0 right-0 flex items-center px-3 text-muted-foreground transition-colors hover:text-foreground"
            aria-label={show ? 'Ocultar senha' : 'Mostrar senha'}
          >
            {show ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        </div>
      </GlassInputWrapper>
    </div>
  )
}
