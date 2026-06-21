'use client'

import Link from 'next/link'
import {
  AlertTriangle,
  BarChart3,
  Bell,
  BellOff,
  DollarSign,
  FileText,
  type LucideIcon,
  ShoppingCart,
} from 'lucide-react'

import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/shared/ui/dropdown-menu'
import { cn } from '@/shared/lib/utils'
import { formatMessage } from '@/shared/i18n/format'
import type { NotificationMessage } from '@/shared/types'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'

type NotificationsDropdownProps = {
  notifications: NotificationMessage[]
  dict: Dictionary
  lang: Locale
  align?: 'start' | 'center' | 'end'
}

const TYPE_META: Record<
  NotificationMessage['type'],
  { icon: LucideIcon; iconColor: string; bgColor: string }
> = {
  NEW_SALE: {
    icon: ShoppingCart,
    iconColor: 'text-blue-600',
    bgColor: 'bg-blue-50',
  },
  ML_PAYMENT_RELEASE: {
    icon: DollarSign,
    iconColor: 'text-emerald-600',
    bgColor: 'bg-emerald-50',
  },
  MONTHLY_CLOSING_SUMMARY: {
    icon: BarChart3,
    iconColor: 'text-amber-600',
    bgColor: 'bg-amber-50',
  },
  WEEKLY_ACCOUNTANT_REPORT: {
    icon: FileText,
    iconColor: 'text-sky-600',
    bgColor: 'bg-sky-50',
  },
  API_INTEGRATION_ALERT: {
    icon: AlertTriangle,
    iconColor: 'text-red-600',
    bgColor: 'bg-red-50',
  },
}

function formatNotificationDate(value: string) {
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function NotificationsDropdown({
  notifications,
  dict,
  lang,
  align = 'end',
}: NotificationsDropdownProps) {
  const unreadCount = notifications.filter((n) => n.status === 'UNREAD').length
  const latest = notifications.slice(0, 5)

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        render={
          <Button
            type="button"
            variant="ghost"
            className="relative h-10 w-10 rounded-xl border border-transparent bg-transparent text-slate-600 transition-all hover:border-slate-200 hover:bg-slate-50 hover:text-slate-900"
            aria-label={dict.header.notifications.ariaLabel}
          >
            <Bell className="size-4" />

            {unreadCount > 0 && (
              <span className="absolute right-1.5 top-1.5 flex size-4 items-center justify-center rounded-full bg-blue-600 text-[10px] font-semibold leading-none text-white ring-2 ring-white">
                {unreadCount > 9 ? '9+' : unreadCount}
              </span>
            )}
          </Button>
        }
      />

      <DropdownMenuContent
        align={align}
        sideOffset={8}
        className="w-[calc(100vw-24px)] max-w-sm rounded-2xl border-slate-200 bg-white p-2 shadow-xl shadow-slate-200/60"
      >
        <DropdownMenuGroup>
          <DropdownMenuLabel className="flex items-center justify-between rounded-xl px-3 py-3">
            <div>
              <p className="text-sm font-semibold text-slate-900">
                {dict.header.notifications.title}
              </p>
              <p className="text-xs font-normal text-slate-500">
                {unreadCount > 0
                  ? formatMessage(dict.header.notifications.newCount, {
                      count: unreadCount,
                    })
                  : dict.header.notifications.empty}
              </p>
            </div>

            {unreadCount > 0 && (
              <Badge className="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-medium text-blue-700 hover:bg-blue-50">
                {unreadCount > 9 ? '9+' : unreadCount}
              </Badge>
            )}
          </DropdownMenuLabel>

          <DropdownMenuSeparator className="my-1 bg-slate-100" />

          {latest.length > 0 ? (
            <div className="max-h-80 overflow-y-auto pr-1 scroll-hidden">
              {latest.map((notification) => {
                const meta = TYPE_META[notification.type]
                const Icon = meta.icon
                const unread = notification.status === 'UNREAD'

                return (
                  <DropdownMenuItem
                    key={notification.id}
                    className={cn(
                      'my-1 flex cursor-pointer items-start justify-between gap-3 rounded-xl px-3 py-3 transition-colors focus:bg-slate-50',
                      unread && 'bg-slate-50/80'
                    )}
                    render={<Link href={`/${lang}/notificacoes`} />}
                  >
                    <div className="flex min-w-0 items-start gap-3">
                      <div className={cn('rounded-xl p-2.5', meta.bgColor)}>
                        <Icon className={cn('size-4.5', meta.iconColor)} />
                      </div>

                      <div className="min-w-0">
                        <div className="flex items-center gap-1.5">
                          <p
                            className={cn(
                              'truncate text-sm text-slate-900',
                              unread ? 'font-semibold' : 'font-medium'
                            )}
                          >
                            {notification.title}
                          </p>

                          {unread && (
                            <span className="size-1.5 shrink-0 rounded-full bg-blue-600" />
                          )}
                        </div>

                        <p className="mt-0.5 line-clamp-2 max-w-52 text-xs leading-5 text-slate-500">
                          {notification.message}
                        </p>
                      </div>
                    </div>

                    <span className="shrink-0 pt-0.5 text-[11px] text-slate-400">
                      {formatNotificationDate(notification.createdAt)}
                    </span>
                  </DropdownMenuItem>
                )
              })}
            </div>
          ) : (
            <div className="flex flex-col items-center gap-2 px-4 py-10 text-center">
              <div className="rounded-2xl bg-slate-50 p-3">
                <BellOff className="size-5 text-slate-400" />
              </div>

              <p className="text-sm font-medium text-slate-900">
                {dict.header.notifications.empty}
              </p>

              <p className="max-w-56 text-xs leading-5 text-slate-500">
                {dict.header.notifications.emptyHint}
              </p>
            </div>
          )}

          <DropdownMenuSeparator className="my-1 bg-slate-100" />

          <div className="p-1">
            <Button
              className="h-10 w-full rounded-xl bg-blue-600 text-sm font-medium text-white shadow-sm transition hover:bg-blue-700"
              nativeButton={false}
              render={<Link href={`/${lang}/notificacoes`} />}
            >
              {dict.header.notifications.viewAll}
            </Button>
          </div>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

export { NotificationsDropdown }
export default NotificationsDropdown