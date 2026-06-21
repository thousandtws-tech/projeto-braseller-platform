'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useState } from 'react'
import {
  LayoutDashboard,
  ShoppingCart,
  Receipt,
  BarChart3,
  Store,
  Bell,
  Settings,
  ChevronLeft,
  ChevronRight,
  LogOut,
  CreditCard,
  UserRound,
  Calculator,
  Package,
  Landmark,
  BriefcaseBusiness,
  Scale,
  Search,
  Activity,
} from 'lucide-react'

import { cn } from '@/shared/lib/utils'
import { isBpoOperator } from '@/entities/session/model/permissions'
import { logoutAction } from '@/features/auth/server/actions'
import { Avatar, AvatarFallback, AvatarImage } from '@/shared/ui/avatar'
import type { UserSession } from '@/shared/types'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'

interface SidebarProps {
  user: UserSession
  dict: Dictionary
  lang: Locale
}

export function Sidebar({ user, dict, lang }: SidebarProps) {
  const pathname = usePathname()
  const [collapsed, setCollapsed] = useState(false)

  const navItems = [
    { href: 'dashboard', label: dict.nav.dashboard, icon: LayoutDashboard },
    { href: 'lancamentos', label: dict.nav.lancamentos, icon: ShoppingCart },
    { href: 'despesas', label: dict.nav.despesas, icon: Receipt },
    { href: 'estoque', label: dict.nav.estoque, icon: Package },
    { href: 'extrato', label: dict.nav.extrato, icon: Landmark },
    { href: 'dre', label: dict.nav.dre, icon: BarChart3 },
    { href: 'balanco', label: dict.nav.balanco, icon: Scale },
    { href: 'conectores', label: dict.nav.conectores, icon: Store },
    { href: 'monitoramento-apis', label: dict.nav.monitoramentoApis, icon: Activity },
  ]

  const items = isBpoOperator(user.roles)
    ? [{ href: 'bpo', label: dict.nav.bpo, icon: BriefcaseBusiness }, ...navItems]
    : navItems

  const bottomNav = [
    { href: 'notificacoes', label: dict.nav.notificacoes, icon: Bell },
    { href: 'contador', label: dict.nav.contador, icon: Calculator },
    { href: 'configuracoes', label: dict.nav.configuracoes, icon: Settings },
    { href: 'plano', label: dict.nav.plano, icon: CreditCard },
  ]

  const initials = user.fullName
    ?.split(' ')
    .slice(0, 2)
    .map((name) => name[0])
    .join('')
    .toUpperCase()

  return (
    <aside
      className={cn(
        'relative flex h-screen flex-col border-r border-slate-200 bg-white text-slate-700 transition-all duration-300 ease-in-out',
        collapsed ? 'w-20' : 'w-72'
      )}
    >
      <div className="flex h-16 items-center justify-between border-b border-slate-200 bg-slate-50/70 px-4">
        <div className={cn('flex items-center gap-3 min-w-0', collapsed && 'mx-auto')}>
          <div className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-blue-600 shadow-sm">
            <span className="text-sm font-bold text-white">B</span>
          </div>

          {!collapsed && (
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-slate-900">Brasaller</p>
              <p className="truncate text-xs text-slate-500">Enterprise Dashboard</p>
            </div>
          )}
        </div>

        {!collapsed && (
          <button
            type="button"
            onClick={() => setCollapsed(true)}
            className="rounded-md p-1.5 text-slate-500 transition hover:bg-slate-100 hover:text-slate-700"
            aria-label={dict.nav.collapseMenu}
          >
            <ChevronLeft className="size-4" />
          </button>
        )}
      </div>

      {collapsed && (
        <button
          type="button"
          onClick={() => setCollapsed(false)}
          className="absolute -right-3 top-5 z-10 flex size-6 items-center justify-center rounded-full border border-slate-200 bg-white shadow-sm transition hover:bg-slate-50"
          aria-label={dict.nav.expandMenu}
        >
          <ChevronRight className="size-3.5 text-slate-500" />
        </button>
      )}

      {!collapsed && (
        <div className="px-4 py-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 size-3.5 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Buscar..."
              className="w-full rounded-lg border border-slate-200 bg-slate-50 py-2 pl-9 pr-3 text-sm outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20"
            />
          </div>
        </div>
      )}

      <nav className="flex-1 overflow-y-auto px-3 py-2 sidebar-scroll">
        <div className="space-y-1">
          {items.map(({ href, label, icon: Icon }) => {
            const fullHref = `/${lang}/${href}`
            const active = pathname === fullHref || pathname.startsWith(`${fullHref}/`)

            return (
              <Link
                key={href}
                href={fullHref}
                title={collapsed ? label : undefined}
                className={cn(
                  'group relative flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition-all',
                  collapsed && 'justify-center px-2',
                  active
                    ? 'bg-blue-50 font-medium text-blue-700'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                )}
              >
                <Icon
                  className={cn(
                    'size-4 shrink-0',
                    active ? 'text-blue-600' : 'text-slate-500 group-hover:text-slate-700'
                  )}
                />

                {!collapsed && <span className="truncate">{label}</span>}
              </Link>
            )
          })}
        </div>
      </nav>

      <div className="border-t border-slate-200 px-3 py-3">
        <div className="space-y-1">
          {bottomNav.map(({ href, label, icon: Icon }) => {
            const fullHref = `/${lang}/${href}`
            const active = pathname === fullHref || pathname.startsWith(`${fullHref}/`)

            return (
              <Link
                key={href}
                href={fullHref}
                title={collapsed ? label : undefined}
                className={cn(
                  'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition-all',
                  collapsed && 'justify-center px-2',
                  active
                    ? 'bg-blue-50 font-medium text-blue-700'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                )}
              >
                <Icon className="size-4 shrink-0" />
                {!collapsed && <span className="truncate">{label}</span>}
              </Link>
            )
          })}
        </div>
      </div>

      <div className="border-t border-slate-200 bg-slate-50/60 p-3">
        <div className={cn('flex items-center gap-3', collapsed && 'justify-center')}>
          <Avatar className="size-9 shrink-0">
            <AvatarImage src={undefined} alt={user.fullName} />
            <AvatarFallback className="bg-slate-200 text-xs font-semibold text-slate-700">
              {initials || <UserRound className="size-4" />}
            </AvatarFallback>
          </Avatar>

          {!collapsed && (
            <>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-slate-900">{user.fullName}</p>
                <p className="truncate text-xs text-slate-500">{user.email}</p>
              </div>

              <form action={logoutAction}>
                <button
                  type="submit"
                  className="rounded-lg p-2 text-slate-500 transition hover:bg-red-50 hover:text-red-600"
                  title={dict.nav.signOut}
                >
                  <LogOut className="size-4" />
                </button>
              </form>
            </>
          )}
        </div>
      </div>
    </aside>
  )
}