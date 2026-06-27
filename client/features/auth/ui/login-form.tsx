'use client'

import Link from 'next/link'
import { useActionState, useEffect, useState } from 'react'
import { useParams } from 'next/navigation'
import { AlertCircle, Eye, EyeOff, Loader2, LockKeyhole } from 'lucide-react'
import { loginAction } from '@/features/auth/server/actions'
import { appToast } from '@/shared/lib/toast'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'

type LoginFormProps = {
  initialEmail?: string
}

export function LoginForm({ initialEmail = '' }: LoginFormProps) {
  const [state, formAction, isPending] = useActionState(loginAction, null)
  const [showPassword, setShowPassword] = useState(false)
  const [email, setEmail] = useState(initialEmail)
  const params = useParams<{ lang: string }>()

  useEffect(() => {
    if (state?.error) {
      appToast.error(state.error)
    }
  }, [state])

  return (
    <form action={formAction} className="flex flex-col gap-5">
      {state?.error ? (
        <div className="flex items-start gap-2.5 rounded-md border border-destructive/25 bg-destructive/5 px-3.5 py-3 text-sm text-destructive">
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>{state.error}</span>
        </div>
      ) : null}

      <div className="flex flex-col gap-2">
        <Label htmlFor="email">E-mail</Label>
        <Input id="email" name="email" type="email" autoComplete="email" placeholder="seu@email.com" required disabled={isPending} aria-invalid={!!state?.error} value={email} onChange={(event) => setEmail(event.target.value)} />
      </div>

      <div className="flex flex-col gap-2">
        <div className="flex items-center justify-between">
          <Label htmlFor="password">Senha</Label>
          <Link href={`/${params.lang}/recuperar-senha`} className="text-xs font-medium text-muted-foreground underline-offset-4 hover:text-foreground hover:underline">
            Esqueceu a senha?
          </Link>
        </div>
        <div className="relative">
          <Input id="password" name="password" type={showPassword ? 'text' : 'password'} autoComplete="current-password" placeholder="••••••••" required disabled={isPending} aria-invalid={!!state?.error} className="pr-11" />
          <button type="button" onClick={() => setShowPassword((value) => !value)} className="absolute inset-y-0 right-0 flex items-center px-3 text-muted-foreground transition hover:text-foreground" aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'}>
            {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        </div>
      </div>

      <Button type="submit" size="lg" className="mt-1 w-full" disabled={isPending}>
        {isPending ? <><Loader2 className="animate-spin" />Entrando...</> : <><LockKeyhole />Entrar com segurança</>}
      </Button>
    </form>
  )
}
