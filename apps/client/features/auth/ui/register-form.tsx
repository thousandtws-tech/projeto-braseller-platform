'use client'

import { useActionState, useState } from 'react'
import { AlertCircle, CheckCircle2, Eye, EyeOff, Loader2 } from 'lucide-react'
import { registerAction } from '@/features/auth/server/actions'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { GlassInputWrapper } from '@/shared/ui/glass-input-wrapper'
import type { Dictionary } from '@/shared/i18n/get-dictionary'

interface Props {
  dict: Dictionary
}

export function RegisterForm({ dict }: Props) {
  const [state, formAction, isPending] = useActionState(registerAction, null)
  const [showPassword, setShowPassword] = useState(false)
  const [password, setPassword] = useState('')
  const form = dict.auth.register.form

  const passwordLongEnough = password.length >= 8

  return (
    <form action={formAction} className="space-y-4">
      {state?.error && (
        <div className="animate-element flex items-start gap-2.5 rounded-lg border border-destructive/30 bg-destructive/8 px-3.5 py-3 text-sm text-destructive">
          <AlertCircle className="size-4 mt-0.5 shrink-0" />
          <span>{state.error}</span>
        </div>
      )}

      <div className="animate-element animate-delay-100 space-y-1.5">
        <Label htmlFor="tenantName">{form.companyName}</Label>
        <GlassInputWrapper>
          <Input
            id="tenantName"
            name="tenantName"
            type="text"
            autoComplete="organization"
            placeholder={form.companyNamePlaceholder}
            required
            disabled={isPending}
            className="h-auto rounded-2xl border-0 bg-transparent px-4 py-3 text-sm focus-visible:ring-0"
          />
        </GlassInputWrapper>
      </div>

      <div className="animate-element animate-delay-200 space-y-1.5">
        <Label htmlFor="fullName">{form.fullName}</Label>
        <GlassInputWrapper>
          <Input
            id="fullName"
            name="fullName"
            type="text"
            autoComplete="name"
            placeholder={form.fullNamePlaceholder}
            required
            disabled={isPending}
            className="h-auto rounded-2xl border-0 bg-transparent px-4 py-3 text-sm focus-visible:ring-0"
          />
        </GlassInputWrapper>
      </div>

      <div className="animate-element animate-delay-300 space-y-1.5">
        <Label htmlFor="email">{form.email}</Label>
        <GlassInputWrapper>
          <Input
            id="email"
            name="email"
            type="email"
            autoComplete="email"
            placeholder={form.emailPlaceholder}
            required
            disabled={isPending}
            className="h-auto rounded-2xl border-0 bg-transparent px-4 py-3 text-sm focus-visible:ring-0"
          />
        </GlassInputWrapper>
      </div>

      <div className="animate-element animate-delay-400 space-y-1.5">
        <Label htmlFor="password">{form.password}</Label>
        <GlassInputWrapper>
          <div className="relative">
            <Input
              id="password"
              name="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              placeholder={form.passwordPlaceholder}
              required
              minLength={8}
              disabled={isPending}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="h-auto rounded-2xl border-0 bg-transparent px-4 py-3 pr-12 text-sm focus-visible:ring-0"
            />
            <button
              type="button"
              tabIndex={-1}
              onClick={() => setShowPassword((value) => !value)}
              className="absolute inset-y-0 right-0 flex items-center px-3 text-muted-foreground hover:text-foreground transition-colors"
              aria-label={showPassword ? form.hidePassword : form.showPassword}
            >
              {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
            </button>
          </div>
        </GlassInputWrapper>
        {password.length > 0 && (
          <p className={`flex items-center gap-1 text-xs ${passwordLongEnough ? 'text-emerald-600 dark:text-emerald-400' : 'text-muted-foreground'}`}>
            <CheckCircle2 className="size-3" />
            {passwordLongEnough ? form.passwordStrong : form.passwordWeak}
          </p>
        )}
      </div>

      <Button type="submit" className="animate-element animate-delay-600 mt-1 h-auto w-full rounded-2xl py-3" disabled={isPending}>
        {isPending ? (
          <>
            <Loader2 className="size-4 animate-spin" />
            {form.submitting}
          </>
        ) : (
          form.submit
        )}
      </Button>

      <p className="animate-element animate-delay-700 text-xs text-center text-muted-foreground">
        {form.termsPrefix}
        <a href="#" className="underline underline-offset-2 hover:text-foreground">
          {form.termsLink}
        </a>
        {form.termsAnd}
        <a href="#" className="underline underline-offset-2 hover:text-foreground">
          {form.privacyLink}
        </a>
        {form.termsSuffix}
      </p>
    </form>
  )
}
