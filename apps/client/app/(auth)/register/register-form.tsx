'use client'

import { useActionState, useState } from 'react'
import { Eye, EyeOff, Loader2, AlertCircle, CheckCircle2 } from 'lucide-react'
import { registerAction } from '@/app/actions/auth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

export function RegisterForm() {
  const [state, formAction, isPending] = useActionState(registerAction, null)
  const [showPassword, setShowPassword] = useState(false)
  const [password, setPassword] = useState('')

  const passwordLongEnough = password.length >= 8

  return (
    <form action={formAction} className="space-y-4">
      {state?.error && (
        <div className="flex items-start gap-2.5 rounded-lg border border-destructive/30 bg-destructive/8 px-3.5 py-3 text-sm text-destructive">
          <AlertCircle className="size-4 mt-0.5 shrink-0" />
          <span>{state.error}</span>
        </div>
      )}

      <div className="space-y-1.5">
        <Label htmlFor="tenantName">Nome da empresa / negócio</Label>
        <Input
          id="tenantName"
          name="tenantName"
          type="text"
          autoComplete="organization"
          placeholder="Ex: Loja do João"
          required
          disabled={isPending}
        />
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="fullName">Nome completo</Label>
        <Input
          id="fullName"
          name="fullName"
          type="text"
          autoComplete="name"
          placeholder="Seu nome completo"
          required
          disabled={isPending}
        />
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="email">E-mail</Label>
        <Input
          id="email"
          name="email"
          type="email"
          autoComplete="email"
          placeholder="seu@email.com"
          required
          disabled={isPending}
        />
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="password">Senha</Label>
        <div className="relative">
          <Input
            id="password"
            name="password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="new-password"
            placeholder="Mínimo 8 caracteres"
            required
            minLength={8}
            disabled={isPending}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="pr-10"
          />
          <button
            type="button"
            tabIndex={-1}
            onClick={() => setShowPassword((v) => !v)}
            className="absolute inset-y-0 right-0 flex items-center px-3 text-muted-foreground hover:text-foreground transition-colors"
            aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'}
          >
            {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        </div>
        {password.length > 0 && (
          <p className={`flex items-center gap-1 text-xs ${passwordLongEnough ? 'text-emerald-600 dark:text-emerald-400' : 'text-muted-foreground'}`}>
            <CheckCircle2 className="size-3" />
            {passwordLongEnough ? 'Senha com 8+ caracteres' : 'Use pelo menos 8 caracteres'}
          </p>
        )}
      </div>

      <Button type="submit" className="w-full mt-1" disabled={isPending}>
        {isPending ? (
          <>
            <Loader2 className="size-4 animate-spin" />
            Criando conta...
          </>
        ) : (
          'Criar conta grátis'
        )}
      </Button>

      <p className="text-xs text-center text-muted-foreground">
        Ao criar uma conta, você concorda com nossos{' '}
        <a href="#" className="underline underline-offset-2 hover:text-foreground">
          Termos de Uso
        </a>{' '}
        e{' '}
        <a href="#" className="underline underline-offset-2 hover:text-foreground">
          Política de Privacidade
        </a>
        .
      </p>
    </form>
  )
}
