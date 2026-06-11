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
  LogOut,
  CreditCard,
  UserRound,
  Calculator,
  Package,
  Landmark,
  BriefcaseBusiness,
  Scale,
} from 'lucide-react'
import { cn } from '@/shared/lib/utils'
import { isBpoOperator } from '@/entities/session/model/permissions'
import { logoutAction } from '@/features/auth/server/actions'
import { Avatar, AvatarImage, AvatarFallback } from '@/shared/ui/avatar'
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
  ]

  const items = isBpoOperator(user.roles)
    ? [{ href: 'bpo', label: dict.nav.bpo, icon: BriefcaseBusiness }, ...navItems]
    : navItems

  const bottomNav = [
    { href: 'notificacoes', label: dict.nav.notificacoes, icon: Bell },
    { href: 'contador', label: dict.nav.contador, icon: Calculator },
    { href: 'configuracoes', label: dict.nav.configuracoes, icon: Settings },
  ]

  return (
    <aside
      className={cn(
        'flex flex-col bg-sidebar text-sidebar-foreground transition-all duration-200 ease-in-out relative',
        collapsed ? 'w-14' : 'w-56'
      )}
    >
      {/* Logo */}
      <div className={cn('flex items-center gap-3 h-14 px-3 border-b border-sidebar-border shrink-0', collapsed && 'justify-center')}>
        <div className="size-8 rounded-lg bg-sidebar-primary flex items-center justify-center shrink-0">
          <span className="text-sidebar-primary-foreground font-bold text-sm">B</span>
        </div>
        {!collapsed && (
          <span className="font-semibold text-base truncate">LogoOficial</span>
        )}
      </div>

      {/* Nav */}
      <nav className="flex-1 py-4 space-y-0.5 px-2 overflow-y-auto">
        {items.map(({ href, label, icon: Icon }) => {
          const fullHref = `/${lang}/${href}`
          const active = pathname === fullHref || pathname.startsWith(fullHref + '/')
          return (
            <Link
              key={href}
              href={fullHref}
              title={collapsed ? label : undefined}
              className={cn(
                'flex items-center gap-3 rounded-lg px-2.5 py-2 text-sm transition-colors',
                collapsed && 'justify-center px-2',
                active
                  ? 'bg-sidebar-accent text-sidebar-accent-foreground font-medium'
                  : 'text-sidebar-foreground/70 hover:bg-sidebar-accent/50 hover:text-sidebar-accent-foreground'
              )}
            >
              <Icon className="size-4 shrink-0" />
              {!collapsed && <span className="truncate">{label}</span>}
            </Link>
          )
        })}
      </nav>

      {/* Bottom nav */}
      <div className="border-t border-sidebar-border py-4 space-y-0.5 px-2">
        {bottomNav.map(({ href, label, icon: Icon }) => {
          const fullHref = `/${lang}/${href}`
          const active = pathname === fullHref
          return (
            <Link
              key={href}
              href={fullHref}
              title={collapsed ? label : undefined}
              className={cn(
                'flex items-center gap-3 rounded-lg px-2.5 py-2 text-sm transition-colors',
                collapsed && 'justify-center px-2',
                active
                  ? 'bg-sidebar-accent text-sidebar-accent-foreground font-medium'
                  : 'text-sidebar-foreground/70 hover:bg-sidebar-accent/50 hover:text-sidebar-accent-foreground'
              )}
            >
              <Icon className="size-4 shrink-0" />
              {!collapsed && <span className="truncate">{label}</span>}
            </Link>
          )
        })}

        <Link
          href={`/${lang}/plano`}
          title={collapsed ? dict.nav.plano : undefined}
          className={cn(
            'flex items-center gap-3 rounded-lg px-2.5 py-2 text-sm transition-colors text-sidebar-foreground/70 hover:bg-sidebar-accent/50 hover:text-sidebar-accent-foreground',
            collapsed && 'justify-center px-2'
          )}
        >
          <CreditCard className="size-4 shrink-0" />
          {!collapsed && <span className="truncate">{dict.nav.plano}</span>}
        </Link>
      </div>

      {/* User footer */}
      <div className={cn('border-t border-sidebar-border p-3', collapsed && 'flex justify-center')}>
        {collapsed ? (
          <Avatar className="size-8">
            <AvatarImage src={undefined} alt={user.fullName} />
            <AvatarFallback className="bg-sidebar-accent">
              <UserRound className="size-4 text-sidebar-accent-foreground" />
            </AvatarFallback>
          </Avatar>
        ) : (
          <div className="flex items-center gap-2">
            <Avatar className="size-8 shrink-0">
              <AvatarImage src={undefined} alt={user.fullName} />
              <AvatarFallback className="bg-sidebar-accent">
                <UserRound className="size-4 text-sidebar-accent-foreground" />
              </AvatarFallback>
            </Avatar>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-medium truncate">{user.fullName}</p>
              <p className="text-xs text-sidebar-foreground/50 truncate">{user.email}</p>
            </div>
            <form action={logoutAction}>
              <button
                type="submit"
                className="p-1 rounded hover:bg-sidebar-accent/50 transition-colors"
                title={dict.nav.signOut}
              >
                <LogOut className="size-3.5 text-sidebar-foreground/50" />
              </button>
            </form>
          </div>
        )}
      </div>

      {/* Collapse toggle */}
      <button
        onClick={() => setCollapsed(!collapsed)}
        className="absolute -right-3 top-[52px] size-6 rounded-full bg-sidebar border border-sidebar-border flex items-center justify-center hover:bg-sidebar-accent transition-colors z-10"
        aria-label={collapsed ? dict.nav.expandMenu : dict.nav.collapseMenu}
      >
        <ChevronLeft className={cn('size-3 text-sidebar-foreground/70 transition-transform', collapsed && 'rotate-180')} />
      </button>
    </aside>
  )
}
