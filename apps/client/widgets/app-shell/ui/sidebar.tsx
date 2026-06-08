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
} from 'lucide-react'
import { cn } from '@/shared/lib/utils'
import { isBpoOperator } from '@/entities/session/model/permissions'
import { logoutAction } from '@/features/auth/server/actions'
import { Avatar, AvatarImage, AvatarFallback } from '@/shared/ui/avatar'
import type { UserSession } from '@/shared/types'

const NAV_ITEMS = [
  { href: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { href: '/lancamentos', label: 'Lançamentos', icon: ShoppingCart },
  { href: '/despesas', label: 'Despesas', icon: Receipt },
  { href: '/estoque', label: 'Estoque', icon: Package },
  { href: '/extrato', label: 'Extrato', icon: Landmark },
  { href: '/dre', label: 'DRE', icon: BarChart3 },
  { href: '/conectores', label: 'Conectores', icon: Store },
]

const BOTTOM_NAV = [
  { href: '/notificacoes', label: 'Notificações', icon: Bell },
  { href: '/contador', label: 'Contador', icon: Calculator },
  { href: '/configuracoes', label: 'Configurações', icon: Settings },
]

interface SidebarProps {
  user: UserSession
}

export function Sidebar({ user }: SidebarProps) {
  const pathname = usePathname()
  const [collapsed, setCollapsed] = useState(false)
  const navItems = isBpoOperator(user.roles)
    ? [{ href: '/bpo', label: 'BPO', icon: BriefcaseBusiness }, ...NAV_ITEMS]
    : NAV_ITEMS

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
          <span className="font-semibold text-base truncate">Brasaller</span>
        )}
      </div>

      {/* Nav */}
      <nav className="flex-1 py-4 space-y-0.5 px-2 overflow-y-auto">
        {navItems.map(({ href, label, icon: Icon }) => {
          const active = pathname === href || pathname.startsWith(href + '/')
          return (
            <Link
              key={href}
              href={href}
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
        {BOTTOM_NAV.map(({ href, label, icon: Icon }) => {
          const active = pathname === href
          return (
            <Link
              key={href}
              href={href}
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
          href="/plano"
          title={collapsed ? 'Plano' : undefined}
          className={cn(
            'flex items-center gap-3 rounded-lg px-2.5 py-2 text-sm transition-colors text-sidebar-foreground/70 hover:bg-sidebar-accent/50 hover:text-sidebar-accent-foreground',
            collapsed && 'justify-center px-2'
          )}
        >
          <CreditCard className="size-4 shrink-0" />
          {!collapsed && <span className="truncate">Plano</span>}
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
                title="Sair"
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
        aria-label={collapsed ? 'Expandir menu' : 'Recolher menu'}
      >
        <ChevronLeft className={cn('size-3 text-sidebar-foreground/70 transition-transform', collapsed && 'rotate-180')} />
      </button>
    </aside>
  )
}
