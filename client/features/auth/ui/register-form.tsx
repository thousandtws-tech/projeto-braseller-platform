'use client'

import { useActionState, useState } from 'react'
import { AlertCircle, Check, Eye, EyeOff, Loader2 } from 'lucide-react'
import { registerAction } from '@/features/auth/server/actions'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'

export function RegisterForm() {
  const [state, formAction, isPending] = useActionState(registerAction, null)
  const [showPassword, setShowPassword] = useState(false)
  const [password, setPassword] = useState('')
  const passwordLongEnough = password.length >= 8

  return (
    <form action={formAction} className="flex flex-col gap-5">
      {state?.error ? (
        <div className="flex items-start gap-2.5 rounded-md border border-destructive/25 bg-destructive/5 px-3.5 py-3 text-sm text-destructive">
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>{state.error}</span>
        </div>
      ) : null}

      <div className="grid gap-5 sm:grid-cols-2">
        <Field id="tenantName" label="Empresa" placeholder="Loja Exemplo" autoComplete="organization" disabled={isPending} />
        <Field id="fullName" label="Nome completo" placeholder="Seu nome" autoComplete="name" disabled={isPending} />
      </div>
      <Field id="email" label="E-mail profissional" placeholder="voce@empresa.com" autoComplete="email" type="email" disabled={isPending} />

      <div className="flex flex-col gap-2">
        <Label htmlFor="password">Senha</Label>
        <div className="relative">
          <Input id="password" name="password" type={showPassword ? 'text' : 'password'} autoComplete="new-password" placeholder="Mínimo de 8 caracteres" required minLength={8} disabled={isPending} value={password} onChange={(event) => setPassword(event.target.value)} className="pr-11" />
          <button type="button" onClick={() => setShowPassword((value) => !value)} className="absolute inset-y-0 right-0 flex items-center px-3 text-muted-foreground transition hover:text-foreground" aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'}>
            {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        </div>
        <div className="grid grid-cols-4 gap-1.5" aria-hidden>
          {Array.from({ length: 4 }).map((_, index) => (
            <span key={index} className={password.length >= (index + 1) * 2 ? 'h-1 rounded-full bg-foreground' : 'h-1 rounded-full bg-muted'} />
          ))}
        </div>
        <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
          <Check className="size-3.5" />
          {passwordLongEnough ? 'Senha pronta para uso' : 'Use pelo menos 8 caracteres'}
        </p>
      </div>

      <Button type="submit" size="lg" className="mt-1 w-full" disabled={isPending || !passwordLongEnough}>
        {isPending ? <><Loader2 className="animate-spin" />Criando conta...</> : 'Criar conta e continuar'}
      </Button>

      <p className="text-center text-xs leading-5 text-muted-foreground">
        Ao continuar, você concorda com os <a href="#" className="text-foreground underline underline-offset-3">Termos de Uso</a> e a <a href="#" className="text-foreground underline underline-offset-3">Política de Privacidade</a>.
      </p>
    </form>
  )
}

function Field({ id, label, ...props }: { id: string; label: string } & React.ComponentProps<typeof Input>) {
  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor={id}>{label}</Label>
      <Input id={id} name={id} required {...props} />
    </div>
  )
}
