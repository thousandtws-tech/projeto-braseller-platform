'use client'

import { useTransition } from 'react'
import {
  Bell, ShoppingCart, DollarSign, FileText, BarChart3,
  CheckCheck, Loader2, BellOff,
} from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { markAsReadAction, clearReadAction } from '@/app/actions/notifications'
import type { NotificationMessage } from '@/types'

const TYPE_META: Record<string, {
  icon: typeof Bell
  bg: string
  label: string
}> = {
  NEW_SALE:                 { icon: ShoppingCart, bg: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400', label: 'Nova venda' },
  ML_PAYMENT_RELEASE:       { icon: DollarSign,   bg: 'bg-amber-500/10 text-amber-600 dark:text-amber-400',      label: 'Pagamento ML' },
  MONTHLY_CLOSING_SUMMARY:  { icon: FileText,      bg: 'bg-blue-500/10 text-blue-600 dark:text-blue-400',         label: 'Fechamento' },
  WEEKLY_ACCOUNTANT_REPORT: { icon: BarChart3,     bg: 'bg-purple-500/10 text-purple-600 dark:text-purple-400',   label: 'Relatório' },
}

function relativeTime(date: string): string {
  const diff = Date.now() - new Date(date).getTime()
  const m = Math.floor(diff / 60000)
  if (m < 1)  return 'agora'
  if (m < 60) return `há ${m} min`
  const h = Math.floor(m / 60)
  if (h < 24) return `há ${h}h`
  const d = Math.floor(h / 24)
  if (d < 7)  return `há ${d} dia${d > 1 ? 's' : ''}`
  return new Date(date).toLocaleDateString('pt-BR')
}

function NotificationItem({
  n,
  onMarkRead,
  isPending,
}: {
  n: NotificationMessage
  onMarkRead: (id: string) => void
  isPending: boolean
}) {
  const meta = TYPE_META[n.type] ?? { icon: Bell, bg: 'bg-muted text-muted-foreground', label: n.type }
  const Icon = meta.icon
  const isUnread = n.status === 'UNREAD'

  return (
    <div className={`
      group flex items-start gap-4 px-5 py-4 transition-colors
      hover:bg-muted/40
      ${isUnread ? 'bg-primary/[0.03]' : ''}
    `}>
      {/* Icon */}
      <div className={`size-9 rounded-xl ${meta.bg} flex items-center justify-center shrink-0 mt-0.5`}>
        <Icon className="size-4" />
      </div>

      {/* Content */}
      <div className="flex-1 min-w-0 space-y-0.5">
        <div className="flex items-center gap-2 flex-wrap">
          <span className={`text-sm leading-snug ${isUnread ? 'font-semibold text-foreground' : 'font-medium text-foreground/80'}`}>
            {n.title}
          </span>
          <Badge variant="secondary" className="text-[10px] px-1.5 py-0 h-4 shrink-0">
            {meta.label}
          </Badge>
        </div>
        <p className="text-xs text-muted-foreground leading-relaxed">{n.message}</p>
        <p className="text-[11px] text-muted-foreground/50 pt-0.5">{relativeTime(n.createdAt)}</p>
      </div>

      {/* Right side */}
      <div className="flex items-center gap-2 shrink-0 self-center">
        {isUnread && (
          <>
            <button
              onClick={() => onMarkRead(n.id)}
              disabled={isPending}
              className="text-[11px] text-primary/70 hover:text-primary opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap disabled:opacity-30"
            >
              Marcar lida
            </button>
            <div className="size-2 rounded-full bg-primary" />
          </>
        )}
      </div>
    </div>
  )
}

interface Props {
  unread: NotificationMessage[]
  read: NotificationMessage[]
}

export function NotificationList({ unread, read }: Props) {
  const [isPending, startTransition] = useTransition()

  function handleMarkRead(id: string) {
    startTransition(() => markAsReadAction(id))
  }

  function handleClearRead() {
    startTransition(() => clearReadAction())
  }

  if (unread.length === 0 && read.length === 0) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center justify-center py-16 gap-4">
          <div className="size-16 rounded-2xl bg-muted flex items-center justify-center">
            <BellOff className="size-7 text-muted-foreground/50" />
          </div>
          <div className="text-center">
            <p className="text-sm font-semibold">Nenhuma notificação</p>
            <p className="text-xs text-muted-foreground mt-1">
              Você será alertado aqui sobre vendas, pagamentos e relatórios.
            </p>
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <div className="space-y-5">
      {/* Unread section */}
      {unread.length > 0 && (
        <div className="space-y-2">
          <div className="flex items-center justify-between px-1">
            <span className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
              Não lidas · {unread.length}
            </span>
          </div>
          <Card className="overflow-hidden">
            <CardContent className="p-0 divide-y divide-border/60">
              {unread.map((n) => (
                <NotificationItem key={n.id} n={n} onMarkRead={handleMarkRead} isPending={isPending} />
              ))}
            </CardContent>
          </Card>
        </div>
      )}

      {/* Read section */}
      {read.length > 0 && (
        <div className="space-y-2">
          <div className="flex items-center justify-between px-1">
            <span className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
              Anteriores · {read.length}
            </span>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleClearRead}
              disabled={isPending}
              className="h-6 px-2 text-xs text-muted-foreground hover:text-foreground"
            >
              {isPending
                ? <Loader2 className="size-3 animate-spin" />
                : <CheckCheck className="size-3" />
              }
              Arquivar lidas
            </Button>
          </div>
          <Card className="overflow-hidden opacity-80">
            <CardContent className="p-0 divide-y divide-border/60">
              {read.map((n) => (
                <NotificationItem key={n.id} n={n} onMarkRead={handleMarkRead} isPending={isPending} />
              ))}
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  )
}
