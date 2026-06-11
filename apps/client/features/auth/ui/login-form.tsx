'use client'

import { useActionState, useState } from 'react'
import { Eye, EyeOff, Loader2, AlertCircle } from 'lucide-react'
import { loginAction } from '@/features/auth/server/actions'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { GlassInputWrapper } from '@/shared/ui/glass-input-wrapper'
import type { Dictionary } from '@/shared/i18n/get-dictionary'

interface Props {
  dict: Dictionary
}

export function LoginForm({ dict }: Props) {
  const [state, formAction, isPending] = useActionState(loginAction, null)
  const [showPassword, setShowPassword] = useState(false)
  const form = dict.auth.login.form

  return (
    <form action={formAction} className="space-y-5">
      {state?.error && (
        <div className="animate-element flex items-start gap-2.5 rounded-lg border border-destructive/30 bg-destructive/8 px-3.5 py-3 text-sm text-destructive">
          <AlertCircle className="size-4 mt-0.5 shrink-0" />
          <span>{state.error}</span>
        </div>
      )}

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
            aria-invalid={!!state?.error}
            className="h-auto rounded-2xl border-0 bg-transparent px-4 py-3 text-sm focus-visible:ring-0"
          />
        </GlassInputWrapper>
      </div>

      <div className="animate-element animate-delay-400 space-y-1.5">
        <div className="flex items-center justify-between">
          <Label htmlFor="password">{form.password}</Label>
          <a
            href="#"
            tabIndex={-1}
            className="text-xs text-muted-foreground hover:text-primary transition-colors"
          >
            {form.forgotPassword}
          </a>
        </div>
        <GlassInputWrapper>
          <div className="relative">
            <Input
              id="password"
              name="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              placeholder={form.passwordPlaceholder}
              required
              disabled={isPending}
              aria-invalid={!!state?.error}
              className="h-auto rounded-2xl border-0 bg-transparent px-4 py-3 pr-12 text-sm focus-visible:ring-0"
            />
            <button
              type="button"
              tabIndex={-1}
              onClick={() => setShowPassword((v) => !v)}
              className="absolute inset-y-0 right-0 flex items-center px-3 text-muted-foreground hover:text-foreground transition-colors"
              aria-label={showPassword ? form.hidePassword : form.showPassword}
            >
              {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
            </button>
          </div>
        </GlassInputWrapper>
      </div>

      <Button type="submit" className="animate-element animate-delay-600 h-auto w-full rounded-2xl py-3" disabled={isPending}>
        {isPending ? (
          <>
            <Loader2 className="size-4 animate-spin" />
            {form.submitting}
          </>
        ) : (
          form.submit
        )}
      </Button>

      <div className="animate-element animate-delay-700 relative flex items-center justify-center">
        <span className="w-full border-t border-border" />
        <span className="absolute bg-background px-2 text-xs text-muted-foreground uppercase">{form.orContinueWith}</span>
      </div>

      <a
        href="/api/auth/google/authorize"
        aria-disabled={isPending}
        className="animate-element animate-delay-800 flex w-full items-center justify-center gap-3 rounded-2xl border border-border py-3 text-sm font-medium transition-colors hover:bg-accent aria-disabled:pointer-events-none aria-disabled:opacity-50"
      >
        <svg viewBox="0 0 24 24" className="size-4" aria-hidden>
          <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
          <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
          <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
          <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
        </svg>
        Google
      </a>
    </form>
  )
}
