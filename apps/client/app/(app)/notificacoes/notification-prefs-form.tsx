'use client'

import { useActionState, useState } from 'react'
import { Loader2, CheckCircle2, AlertCircle, ShoppingCart, DollarSign, FileText, BarChart3, Mail } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Separator } from '@/components/ui/separator'
import { updatePreferencesAction } from '@/app/actions/notifications'
import type { NotificationPreferences } from '@/types'

const PREFS_CONFIG = [
  {
    key: 'newSaleEnabled',
    icon: ShoppingCart,
    label: 'Nova venda',
    desc: 'Notificado a cada nova venda confirmada nos marketplaces',
    color: 'text-emerald-600',
    bg: 'bg-emerald-500/10',
  },
  {
    key: 'mlPaymentReleaseEnabled',
    icon: DollarSign,
    label: 'Liberação de pagamento ML',
    desc: 'Alertas quando um pagamento do Mercado Livre está próximo de liberar',
    color: 'text-amber-600',
    bg: 'bg-amber-500/10',
  },
  {
    key: 'monthlyClosingEnabled',
    icon: FileText,
    label: 'Fechamento mensal',
    desc: 'Resumo automático com totais de vendas e receita do mês',
    color: 'text-blue-600',
    bg: 'bg-blue-500/10',
  },
  {
    key: 'weeklyAccountantReportEnabled',
    icon: BarChart3,
    label: 'Relatório semanal ao contador',
    desc: 'Envio semanal automático com resumo de vendas para o contador',
    color: 'text-purple-600',
    bg: 'bg-purple-500/10',
  },
] as const

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
}

export function NotificationPrefsForm({ prefs }: Props) {
  const [state, formAction, isPending] = useActionState(updatePreferencesAction, null)

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
    <form action={formAction} className="space-y-6">
      {state?.success === false && (
        <div className="flex items-start gap-2.5 rounded-lg border border-destructive/30 bg-destructive/8 px-3.5 py-3 text-sm text-destructive">
          <AlertCircle className="size-4 mt-0.5 shrink-0" />
          <span>{state.error}</span>
        </div>
      )}
      {state?.success === true && (
        <div className="flex items-start gap-2.5 rounded-lg border border-emerald-500/30 bg-emerald-500/8 px-3.5 py-3 text-sm text-emerald-700 dark:text-emerald-400">
          <CheckCircle2 className="size-4 mt-0.5 shrink-0" />
          <span>Preferências salvas com sucesso.</span>
        </div>
      )}

      {/* Canal e-mail */}
      <div className="flex items-center justify-between gap-6 py-1">
        <div className="flex items-center gap-3">
          <div className="size-9 rounded-xl bg-muted flex items-center justify-center shrink-0">
            <Mail className="size-4 text-muted-foreground" />
          </div>
          <div>
            <p className="text-sm font-medium">Notificações por e-mail</p>
            <p className="text-xs text-muted-foreground">Receber alertas no e-mail além do app</p>
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
      <div className="space-y-5">
        <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">Tipos de alerta</p>
        {PREFS_CONFIG.map(({ key, icon: Icon, label, desc, color, bg }) => (
          <div key={key} className="flex items-center justify-between gap-6">
            <div className="flex items-center gap-3">
              <div className={`size-9 rounded-xl ${bg} flex items-center justify-center shrink-0`}>
                <Icon className={`size-4 ${color}`} />
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
      <div className="space-y-4">
        <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">Destinatários</p>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="space-y-1.5">
            <Label htmlFor="recipientEmail">E-mail do destinatário</Label>
            <Input
              id="recipientEmail"
              name="recipientEmail"
              type="email"
              value={recipientEmail}
              onChange={(e) => setRecipientEmail(e.target.value)}
              placeholder="seu@email.com.br"
              disabled={isPending}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="accountantEmail">E-mail do contador</Label>
            <Input
              id="accountantEmail"
              name="accountantEmail"
              type="email"
              value={accountantEmail}
              onChange={(e) => setAccountantEmail(e.target.value)}
              placeholder="contador@escritorio.com.br"
              disabled={isPending}
            />
          </div>
        </div>
      </div>

      <div className="flex justify-end">
        <Button type="submit" disabled={isPending}>
          {isPending
            ? <><Loader2 className="size-4 animate-spin" />Salvando...</>
            : 'Salvar preferências'
          }
        </Button>
      </div>
    </form>
  )
}
