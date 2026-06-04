'use client'

import { useActionState, useState } from 'react'
import { Loader2, AlertCircle, CheckCircle2, Eye, EyeOff } from 'lucide-react'
import { grantAccountantAction } from '@/app/actions/users'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

export function AccountantForm() {
  const [state, formAction, isPending] = useActionState(grantAccountantAction, null)
  const [showPassword, setShowPassword] = useState(false)

  return (
    <form action={formAction} className="space-y-4">
      {state?.success === false && (
        <div className="flex items-start gap-2.5 rounded-lg border border-destructive/30 bg-destructive/8 px-3.5 py-3 text-sm text-destructive">
          <AlertCircle className="size-4 mt-0.5 shrink-0" />
          <span>{state.error}</span>
        </div>
      )}
      {state?.success === true && (
        <div className="flex items-start gap-2.5 rounded-lg border border-emerald-500/30 bg-emerald-500/8 px-3.5 py-3 text-sm text-emerald-700 dark:text-emerald-400">
          <CheckCircle2 className="size-4 mt-0.5 shrink-0" />
          <span>{state.message}</span>
        </div>
      )}

      <p className="text-sm text-muted-foreground">
        O contador terá acesso de leitura às informações financeiras do tenant com papel <strong>CONTADOR</strong>.
      </p>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div className="space-y-1.5">
          <Label htmlFor="accountantFirstName">Nome</Label>
          <Input
            id="accountantFirstName"
            name="accountantFirstName"
            type="text"
            autoComplete="off"
            placeholder="João"
            required
            disabled={isPending}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="accountantLastName">Sobrenome</Label>
          <Input
            id="accountantLastName"
            name="accountantLastName"
            type="text"
            autoComplete="off"
            placeholder="Silva"
            required
            disabled={isPending}
          />
        </div>
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="accountantEmail">E-mail</Label>
        <Input
          id="accountantEmail"
          name="accountantEmail"
          type="email"
          autoComplete="off"
          placeholder="contador@escritorio.com.br"
          required
          disabled={isPending}
        />
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="accountantPassword">Senha temporária</Label>
        <div className="relative">
          <Input
            id="accountantPassword"
            name="accountantPassword"
            type={showPassword ? 'text' : 'password'}
            autoComplete="new-password"
            placeholder="Mínimo 8 caracteres"
            required
            minLength={8}
            disabled={isPending}
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
        <p className="text-xs text-muted-foreground">
          O contador deverá trocar a senha no primeiro acesso.
        </p>
      </div>

      <Button type="submit" size="sm" disabled={isPending}>
        {isPending ? (
          <>
            <Loader2 className="size-4 animate-spin" />
            Concedendo acesso...
          </>
        ) : (
          'Conceder acesso'
        )}
      </Button>
    </form>
  )
}
