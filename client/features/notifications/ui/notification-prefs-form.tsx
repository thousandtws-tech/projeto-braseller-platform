'use client'

import { useActionState, useState } from 'react'
import { Loader2, CheckCircle2, AlertCircle, ShoppingCart, DollarSign, FileText, BarChart3, Mail } from 'lucide-react'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { Separator } from '@/shared/ui/separator'
import { updatePreferencesAction } from '@/features/notifications/server/actions'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { NotificationPreferences } from '@/shared/types'

interface ToggleProps {
  checked: boolean
  onChange: () => void
  name: string
  disabled?: boolean
}

function Toggle({ checked, onChange, name, disabled }: ToggleProps) {
  return (
    <>
      <input type="hidden" name={name} value={checked ? 'true' : 'false'} />
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        onClick={onChange}
        disabled={disabled}
        className={`
          relative inline-flex h-6 w-11 shrink-0 items-center rounded-full
          transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring
          disabled:opacity-50
          ${checked ? 'bg-primary' : 'bg-muted-foreground/25'}
        `}
      >
        <span className={`
          inline-block size-4 rounded-full bg-white shadow-sm transition-transform
          ${checked ? 'translate-x-6' : 'translate-x-1'}
        `} />
      </button>
    </>
  )
}

interface Props {
  prefs: NotificationPreferences | null
  dict: Dictionary
}

export function NotificationPrefsForm({ prefs, dict }: Props) {
  const [state, formAction, isPending] = useActionState(updatePreferencesAction, null)

  const PREFS_CONFIG = [
    {
      key: 'newSaleEnabled' as const,
      icon: ShoppingCart,
      label: dict.notifications.preferences.items.newSale.label,
      desc: dict.notifications.preferences.items.newSale.desc,
    },
    {
      key: 'mlPaymentReleaseEnabled' as const,
      icon: DollarSign,
      label: dict.notifications.preferences.items.mlPaymentRelease.label,
      desc: dict.notifications.preferences.items.mlPaymentRelease.desc,
    },
    {
      key: 'monthlyClosingEnabled' as const,
      icon: FileText,
      label: dict.notifications.preferences.items.monthlyClosing.label,
      desc: dict.notifications.preferences.items.monthlyClosing.desc,
    },
    {
      key: 'weeklyAccountantReportEnabled' as const,
      icon: BarChart3,
      label: dict.notifications.preferences.items.weeklyAccountantReport.label,
      desc: dict.notifications.preferences.items.weeklyAccountantReport.desc,
    },
  ]

  const [values, setValues] = useState({
    emailEnabled:                  prefs?.emailEnabled ?? true,
    newSaleEnabled:                prefs?.newSaleEnabled ?? true,
    monthlyClosingEnabled:         prefs?.monthlyClosingEnabled ?? true,
    mlPaymentReleaseEnabled:       prefs?.mlPaymentReleaseEnabled ?? true,
    weeklyAccountantReportEnabled: prefs?.weeklyAccountantReportEnabled ?? false,
  })
  const [recipientEmail, setRecipientEmail]   = useState(prefs?.recipientEmail ?? '')
  const [accountantEmail, setAccountantEmail] = useState(prefs?.accountantEmail ?? '')

  function toggle(key: keyof typeof values) {
    setValues((v) => ({ ...v, [key]: !v[key] }))
  }

  return (
    <form action={formAction} className="flex flex-col gap-6">
      {state?.success === false && (
        <div className="flex items-start gap-2.5 rounded-lg border border-destructive/30 bg-destructive/8 px-3.5 py-3 text-sm text-destructive">
          <AlertCircle className="size-4 mt-0.5 shrink-0" />
          <span>{state.error}</span>
        </div>
      )}
      {state?.success === true && (
        <div className="flex items-start gap-2.5 rounded-lg border border-emerald-500/30 bg-emerald-500/8 px-3.5 py-3 text-sm text-emerald-700 dark:text-emerald-400">
          <CheckCircle2 className="size-4 mt-0.5 shrink-0" />
          <span>{dict.notifications.preferences.success}</span>
        </div>
      )}

      {/* Canal e-mail */}
      <div className="flex items-center justify-between gap-6 py-1">
        <div className="flex items-center gap-3">
          <div className="flex size-9 shrink-0 items-center justify-center rounded-md border border-border bg-background">
            <Mail className="size-4 text-muted-foreground" />
          </div>
          <div>
            <p className="text-sm font-medium">{dict.notifications.preferences.email.title}</p>
            <p className="text-xs text-muted-foreground">{dict.notifications.preferences.email.subtitle}</p>
          </div>
        </div>
        <Toggle
          name="emailEnabled"
          checked={values.emailEnabled}
          onChange={() => toggle('emailEnabled')}
          disabled={isPending}
        />
      </div>

      <Separator />

      {/* Alert types */}
      <div className="flex flex-col gap-5">
        <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">{dict.notifications.preferences.alertTypes}</p>
        {PREFS_CONFIG.map(({ key, icon: Icon, label, desc }) => (
          <div key={key} className="flex items-center justify-between gap-6">
            <div className="flex items-center gap-3">
              <div className="flex size-9 shrink-0 items-center justify-center rounded-md border border-border bg-background">
                <Icon className="size-4 text-muted-foreground" />
              </div>
              <div>
                <p className="text-sm font-medium">{label}</p>
                <p className="text-xs text-muted-foreground leading-snug max-w-xs">{desc}</p>
              </div>
            </div>
            <Toggle
              name={key}
              checked={values[key]}
              onChange={() => toggle(key)}
              disabled={isPending}
            />
          </div>
        ))}
      </div>

      <Separator />

      {/* E-mails */}
      <div className="flex flex-col gap-4">
        <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">{dict.notifications.preferences.recipients}</p>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="flex flex-col gap-2">
            <Label htmlFor="recipientEmail">{dict.notifications.preferences.recipientEmail}</Label>
            <Input
              id="recipientEmail"
              name="recipientEmail"
              type="email"
              value={recipientEmail}
              onChange={(e) => setRecipientEmail(e.target.value)}
              placeholder={dict.notifications.preferences.recipientPlaceholder}
              disabled={isPending}
            />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="accountantEmail">{dict.notifications.preferences.accountantEmail}</Label>
            <Input
              id="accountantEmail"
              name="accountantEmail"
              type="email"
              value={accountantEmail}
              onChange={(e) => setAccountantEmail(e.target.value)}
              placeholder={dict.notifications.preferences.accountantPlaceholder}
              disabled={isPending}
            />
          </div>
        </div>
      </div>

      <div className="flex justify-end">
        <Button type="submit" size="lg" className="w-full" disabled={isPending}>
          {isPending
            ? <><Loader2 className="size-4 animate-spin" />{dict.notifications.preferences.saving}</>
            : dict.notifications.preferences.submit
          }
        </Button>
      </div>
    </form>
  )
}
