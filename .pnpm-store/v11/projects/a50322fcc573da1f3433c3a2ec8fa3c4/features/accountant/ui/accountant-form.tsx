'use client'

import { useActionState, useState } from 'react'
import { Loader2, AlertCircle, CheckCircle2, Eye, EyeOff } from 'lucide-react'
import { grantAccountantAction } from '@/features/accountant/server/actions'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { ReadOnlyLock } from '@/shared/ui/read-only-lock'
import type { Dictionary } from '@/shared/i18n/get-dictionary'

interface Props {
  readOnly?: boolean
  dict: Dictionary
}

export function AccountantForm({ readOnly = false, dict }: Props) {
  const [state, formAction, isPending] = useActionState(grantAccountantAction, null)
  const [showPassword, setShowPassword] = useState(false)
  const disabled = readOnly || isPending
  const fields = dict.accountant.grantForm.fields

  return (
    <form action={formAction} className="flex flex-col gap-4">
      {readOnly && <ReadOnlyLock />}
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

      <p className="text-xs leading-5 text-muted-foreground">
        {dict.accountant.grantForm.descriptionPrefix}
        <strong>{dict.accountant.grantForm.descriptionRole}</strong>
        {dict.accountant.grantForm.descriptionSuffix}
      </p>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="flex flex-col gap-2">
          <Label htmlFor="accountantFirstName">{fields.firstName}</Label>
          <Input
            id="accountantFirstName"
            name="accountantFirstName"
            type="text"
            autoComplete="off"
            placeholder={fields.firstNamePlaceholder}
            required
            disabled={disabled}
          />
        </div>
        <div className="flex flex-col gap-2">
          <Label htmlFor="accountantLastName">{fields.lastName}</Label>
          <Input
            id="accountantLastName"
            name="accountantLastName"
            type="text"
            autoComplete="off"
            placeholder={fields.lastNamePlaceholder}
            required
            disabled={disabled}
          />
        </div>
      </div>

      <div className="flex flex-col gap-2">
        <Label htmlFor="accountantEmail">{fields.email}</Label>
        <Input
          id="accountantEmail"
          name="accountantEmail"
          type="email"
          autoComplete="off"
          placeholder={fields.emailPlaceholder}
          required
          disabled={disabled}
        />
      </div>

      <div className="flex flex-col gap-2">
        <Label htmlFor="accountantPassword">{fields.password}</Label>
        <div className="relative">
          <Input
            id="accountantPassword"
            name="accountantPassword"
            type={showPassword ? 'text' : 'password'}
            autoComplete="new-password"
            placeholder={fields.passwordPlaceholder}
            required
            minLength={8}
            disabled={disabled}
            className="pr-10"
          />
          <button
            type="button"
            tabIndex={-1}
            onClick={() => {
              if (!readOnly) setShowPassword((v) => !v)
            }}
            disabled={readOnly}
            className="absolute inset-y-0 right-0 flex items-center px-3 text-muted-foreground hover:text-foreground transition-colors"
            aria-label={showPassword ? dict.accountant.grantForm.hidePassword : dict.accountant.grantForm.showPassword}
          >
            {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        </div>
        <p className="text-xs text-muted-foreground">
          {dict.accountant.grantForm.passwordHint}
        </p>
      </div>

      <Button type="submit" size="lg" className="w-full" disabled={disabled}>
        {isPending ? (
          <>
            <Loader2 className="size-4 animate-spin" />
            {dict.accountant.grantForm.submitting}
          </>
        ) : (
          dict.accountant.grantForm.submit
        )}
      </Button>
    </form>
  )
}
