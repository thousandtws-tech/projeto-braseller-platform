'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useState } from 'react'
import {
  BarChart3,
  Bell,
  BriefcaseBusiness,
  Calculator,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  CircleHelp,
  Landmark,
  LayoutDashboard,
  LogOut,
  Package,
  Receipt,
  Scale,
  Search,
  Settings,
  ShoppingCart,
  Store,
  UserRound,
  WalletCards,
  Waypoints,
} from 'lucide-react'

import { cn } from '@/shared/lib/utils'
import { isBpoOperator } from '@/entities/session/model/permissions'
import { logoutAction } from '@/features/auth/server/actions'
import { Avatar, AvatarFallback, AvatarImage } from '@/shared/ui/avatar'
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/shared/ui/collapsible'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from '@/shared/ui/dropdown-menu'
import type { UserSession } from '@/shared/types'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'

interface SidebarProps {
  user: UserSession
  dict: Dictionary
  lang: Locale
}

type NavigationEntry = {
  href: string
  label: string
  icon: React.ComponentType<{ className?: string }>
}

type NavigationGroup = {
  id: string
  label: string
  icon: React.ComponentType<{ className?: string }>
  items: NavigationEntry[]
}

export function Sidebar({ user, dict, lang }: SidebarProps) {
  const pathname = usePathname()
  const [collapsed, setCollapsed] = useState(false)
  const navigation = createNavigation(dict)

  const initials = user.fullName
    ?.split(' ')
    .slice(0, 2)
    .map((name) => name[0])
    .join('')
    .toUpperCase()

  return (
    <aside
      className={cn(
        'relative hidden h-dvh shrink-0 flex-col border-r border-sidebar-border bg-sidebar text-sidebar-foreground transition-[width] duration-200 ease-out lg:flex',
        collapsed ? 'w-[72px]' : 'w-[232px]'
      )}
    >
      <div className="flex h-[72px] items-center justify-between border-b border-sidebar-border px-4">
        <div className={cn('flex min-w-0 items-center gap-3', collapsed && 'mx-auto')}>
          <div className="flex size-8 shrink-0 items-center justify-center rounded-md bg-sidebar-primary">
            <span className="text-sm font-bold text-sidebar-primary-foreground">B</span>
          </div>
          {collapsed ? null : (
            <div className="min-w-0">
              <p className="truncate text-[15px] font-semibold tracking-[-0.025em] text-foreground">
                Logo
              </p>
              <p className="truncate text-[10px] uppercase tracking-[0.12em] text-muted-foreground">
                SubLine
              </p>
            </div>
          )}
        </div>

        {collapsed ? null : (
          <button
            type="button"
            onClick={() => setCollapsed(true)}
            className="rounded-md p-1.5 text-muted-foreground transition hover:bg-sidebar-accent hover:text-foreground"
            aria-label={dict.nav.collapseMenu}
          >
            <ChevronLeft className="size-4" />
          </button>
        )}
      </div>

      {collapsed ? (
        <button
          type="button"
          onClick={() => setCollapsed(false)}
          className="absolute -right-3 top-6 z-10 flex size-6 items-center justify-center rounded-full border border-border bg-background transition hover:bg-muted"
          aria-label={dict.nav.expandMenu}
        >
          <ChevronRight className="size-3.5 text-muted-foreground" />
        </button>
      ) : null}

      {collapsed ? null : (
        <div className="px-3 py-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
            <input
              type="search"
              placeholder={dict.header.searchPlaceholder}
              className="h-9 w-full rounded-md border border-border bg-muted/45 py-2 pl-9 pr-3 text-xs outline-none transition placeholder:text-muted-foreground focus:border-foreground/30 focus:bg-background"
            />
          </div>
        </div>
      )}

      <nav className="sidebar-scroll flex-1 overflow-y-auto px-2 py-2">
        <div className="flex flex-col gap-1">
          <NavigationItem
            href="dashboard"
            label={dict.nav.dashboard}
            icon={LayoutDashboard}
            lang={lang}
            pathname={pathname}
            collapsed={collapsed}
          />

          {isBpoOperator(user.roles) ? (
            <NavigationItem
              href="bpo"
              label={dict.nav.bpo}
              icon={BriefcaseBusiness}
              lang={lang}
              pathname={pathname}
              collapsed={collapsed}
            />
          ) : null}

          <div className={cn('my-2 border-t border-sidebar-border', collapsed && 'mx-2')} />

          {navigation.map((group) => (
            <SidebarGroup
              key={group.id}
              group={group}
              lang={lang}
              pathname={pathname}
              collapsed={collapsed}
            />
          ))}
        </div>
      </nav>

      <div className="border-t border-sidebar-border px-2 py-2">
        <a
          href="mailto:suporte@brasaller.com"
          title={collapsed ? 'Ajuda' : undefined}
          className={cn(
            'flex items-center gap-3 rounded-md px-3 py-2.5 text-[13px] text-sidebar-foreground transition hover:bg-sidebar-accent hover:text-foreground',
            collapsed && 'justify-center px-2'
          )}
        >
          <CircleHelp className="size-4 text-muted-foreground" />
          {collapsed ? null : 'Ajuda'}
        </a>
      </div>

      <div className="border-t border-sidebar-border p-3">
        <div className={cn('flex items-center gap-3', collapsed && 'justify-center')}>
          <Avatar className="size-9 shrink-0">
            <AvatarImage src={undefined} alt={user.fullName} />
            <AvatarFallback className="bg-foreground text-xs font-semibold text-background">
              {initials || <UserRound className="size-4" />}
            </AvatarFallback>
          </Avatar>

          {collapsed ? null : (
            <>
              <div className="min-w-0 flex-1">
                <p className="truncate text-xs font-medium text-foreground">{user.fullName}</p>
                <p className="truncate text-[11px] text-muted-foreground">{user.email}</p>
              </div>
              <form action={logoutAction}>
                <button
                  type="submit"
                  className="rounded-md p-2 text-muted-foreground transition hover:bg-destructive/10 hover:text-destructive"
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

function SidebarGroup({
  group,
  lang,
  pathname,
  collapsed,
}: {
  group: NavigationGroup
  lang: Locale
  pathname: string
  collapsed: boolean
}) {
  const active = group.items.some((item) => isRouteActive(pathname, lang, item.href))
  const [userOpen, setUserOpen] = useState(group.id === 'operation')
  const open = active || userOpen

  if (collapsed) {
    return (
      <DropdownMenu>
        <DropdownMenuTrigger
          className={cn(
            'flex w-full items-center justify-center rounded-md px-2 py-2.5 text-muted-foreground transition hover:bg-sidebar-accent hover:text-foreground',
            active && 'bg-sidebar-accent text-foreground'
          )}
          aria-label={group.label}
          title={group.label}
        >
          <group.icon className="size-4" />
        </DropdownMenuTrigger>
        <DropdownMenuContent side="right" align="start" sideOffset={8} className="w-56">
          <DropdownMenuGroup>
            <DropdownMenuLabel className="text-[11px] uppercase tracking-[0.12em] text-muted-foreground">
              {group.label}
            </DropdownMenuLabel>
            {group.items.map((item) => (
              <DropdownMenuItem
                key={item.href}
                render={<Link href={`/${lang}/${item.href}`} />}
                className={cn(isRouteActive(pathname, lang, item.href) && 'bg-muted font-medium')}
              >
                <item.icon className="size-4 text-muted-foreground" />
                {item.label}
              </DropdownMenuItem>
            ))}
          </DropdownMenuGroup>
        </DropdownMenuContent>
      </DropdownMenu>
    )
  }

  return (
    <Collapsible open={open} onOpenChange={setUserOpen} className="group/nav-group">
      <CollapsibleTrigger
        className={cn(
          'flex w-full items-center gap-3 rounded-md px-3 py-2.5 text-left text-[13px] font-medium transition hover:bg-sidebar-accent hover:text-foreground',
          active ? 'text-foreground' : 'text-sidebar-foreground'
        )}
      >
        <group.icon className={cn('size-4 shrink-0', active ? 'text-foreground' : 'text-muted-foreground')} />
        <span className="min-w-0 flex-1 truncate">{group.label}</span>
        <ChevronDown className="size-3.5 text-muted-foreground transition-transform duration-300 ease-[cubic-bezier(0.22,1,0.36,1)] group-data-open/nav-group:rotate-180" />
      </CollapsibleTrigger>
      <CollapsibleContent className="sidebar-group-content overflow-hidden">
        <div className="relative ml-5 flex flex-col gap-0.5 border-l border-sidebar-border py-1 pl-3">
          {group.items.map((item) => (
            <NavigationItem
              key={item.href}
              {...item}
              lang={lang}
              pathname={pathname}
              collapsed={false}
              nested
            />
          ))}
        </div>
      </CollapsibleContent>
    </Collapsible>
  )
}

function NavigationItem({
  href,
  label,
  icon: Icon,
  lang,
  pathname,
  collapsed,
  nested = false,
}: NavigationEntry & {
  lang: Locale
  pathname: string
  collapsed: boolean
  nested?: boolean
}) {
  const fullHref = `/${lang}/${href}`
  const active = isRouteActive(pathname, lang, href)

  return (
    <Link
      href={fullHref}
      title={collapsed ? label : undefined}
      className={cn(
        'group flex items-center gap-3 rounded-md px-3 py-2.5 text-[13px] transition-colors',
        collapsed && 'justify-center px-2',
        nested && 'py-2 text-xs',
        active
          ? 'bg-sidebar-accent font-medium text-foreground'
          : 'text-sidebar-foreground hover:bg-sidebar-accent/70 hover:text-foreground'
      )}
    >
      <Icon className={cn('size-4 shrink-0', nested && 'size-3.5', active ? 'text-foreground' : 'text-muted-foreground')} />
      {collapsed ? null : <span className="truncate">{label}</span>}
    </Link>
  )
}

function createNavigation(dict: Dictionary): NavigationGroup[] {
  return [
    {
      id: 'operation',
      label: 'Operação',
      icon: Waypoints,
      items: [
        { href: 'lancamentos', label: dict.nav.lancamentos, icon: ShoppingCart },
        { href: 'despesas', label: dict.nav.despesas, icon: Receipt },
        { href: 'estoque', label: dict.nav.estoque, icon: Package },
        { href: 'extrato', label: dict.nav.extrato, icon: Landmark },
      ],
    },
    {
      id: 'finance',
      label: 'Financeiro',
      icon: WalletCards,
      items: [
        { href: 'dre', label: dict.nav.dre, icon: BarChart3 },
        { href: 'balanco', label: dict.nav.balanco, icon: Scale },
      ],
    },
    {
      id: 'ecosystem',
      label: 'Ecossistema',
      icon: Store,
      items: [
        { href: 'conectores', label: dict.nav.conectores, icon: Store },
        { href: 'contador', label: dict.nav.contador, icon: Calculator },
        { href: 'notificacoes', label: dict.nav.notificacoes, icon: Bell },
      ],
    },
    {
      id: 'administration',
      label: 'Administração',
      icon: Settings,
      items: [
        { href: 'configuracoes', label: dict.nav.configuracoes, icon: Settings },
      ],
    },
  ]
}

function isRouteActive(pathname: string, lang: Locale, href: string) {
  const fullHref = `/${lang}/${href}`
  return pathname === fullHref || pathname.startsWith(`${fullHref}/`)
}
