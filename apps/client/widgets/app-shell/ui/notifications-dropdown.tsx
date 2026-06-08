"use client"

import Link from "next/link"
import {
  BarChart3,
  Bell,
  BellOff,
  DollarSign,
  FileText,
  type LucideIcon,
  ShoppingCart,
} from "lucide-react"

import { Badge } from "@/shared/ui/badge"
import { Button } from "@/shared/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu"
import { cn } from "@/shared/lib/utils"
import type { NotificationMessage } from "@/shared/types"

type NotificationsDropdownProps = {
  notifications: NotificationMessage[]
  align?: "start" | "center" | "end"
}

const TYPE_META: Record<
  NotificationMessage["type"],
  { icon: LucideIcon; iconColor: string; bgColor: string }
> = {
  NEW_SALE: {
    icon: ShoppingCart,
    iconColor: "text-primary",
    bgColor: "bg-primary/10",
  },
  ML_PAYMENT_RELEASE: {
    icon: DollarSign,
    iconColor: "text-emerald-600 dark:text-emerald-400",
    bgColor: "bg-emerald-500/10",
  },
  MONTHLY_CLOSING_SUMMARY: {
    icon: BarChart3,
    iconColor: "text-amber-600 dark:text-amber-400",
    bgColor: "bg-amber-500/10",
  },
  WEEKLY_ACCOUNTANT_REPORT: {
    icon: FileText,
    iconColor: "text-sky-600 dark:text-sky-400",
    bgColor: "bg-sky-500/10",
  },
}

function formatNotificationDate(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value))
}

function NotificationsDropdown({
  notifications,
  align = "end",
}: NotificationsDropdownProps) {
  const unreadCount = notifications.filter((n) => n.status === "UNREAD").length
  const latest = notifications.slice(0, 5)

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        render={
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            className="relative"
            aria-label="Notificações"
          >
            <Bell />
            {unreadCount > 0 && (
              <span className="absolute -top-0.5 -right-0.5 size-4 rounded-full bg-primary text-primary-foreground text-[10px] flex items-center justify-center font-semibold leading-none">
                {unreadCount > 9 ? "9+" : unreadCount}
              </span>
            )}
          </Button>
        }
      />
      <DropdownMenuContent
        align={align}
        className="w-[calc(100vw-24px)] max-w-sm p-0 rounded-2xl data-open:slide-in-from-top-2 data-closed:fade-out-0"
      >
        <DropdownMenuGroup>
          <DropdownMenuLabel className="flex items-center justify-between p-4">
            <span className="text-base font-medium text-popover-foreground">
              Notificações
            </span>
            {unreadCount > 0 && (
              <Badge className="font-normal">{unreadCount} novas</Badge>
            )}
          </DropdownMenuLabel>

          {latest.length > 0 ? (
            latest.map((notification) => {
              const meta = TYPE_META[notification.type]
              const Icon = meta.icon

              return (
                <DropdownMenuItem
                  key={notification.id}
                  className="mx-1.5 my-1 flex cursor-pointer items-center justify-between gap-3 p-2"
                  render={<Link href="/notificacoes" />}
                >
                  <div className="flex min-w-0 items-center gap-3">
                    <div className={cn("rounded-xl p-2.5", meta.bgColor)}>
                      <Icon className={cn("size-5", meta.iconColor)} />
                    </div>
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-popover-foreground">
                        {notification.title}
                      </p>
                      <p className="max-w-52 truncate text-sm text-muted-foreground">
                        {notification.message}
                      </p>
                    </div>
                  </div>
                  <span className="shrink-0 text-xs text-muted-foreground">
                    {formatNotificationDate(notification.createdAt)}
                  </span>
                </DropdownMenuItem>
              )
            })
          ) : (
            <div className="flex flex-col items-center gap-2 px-4 py-8 text-center">
              <div className="rounded-xl bg-muted p-3">
                <BellOff className="size-5 text-muted-foreground" />
              </div>
              <p className="text-sm font-medium">Nenhuma notificação</p>
              <p className="text-xs text-muted-foreground">
                Tudo em dia por aqui.
              </p>
            </div>
          )}

          <div className="mx-1.5 my-1 p-2">
            <Button className="h-9 w-full rounded-xl" nativeButton={false} render={<Link href="/notificacoes" />}>
              Ver todas as notificações
            </Button>
          </div>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

export { NotificationsDropdown }
export default NotificationsDropdown
