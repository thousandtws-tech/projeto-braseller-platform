'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import {
  BarChart3,
  Bell,
  Building2,
  Calculator,
  ChevronDown,
  CreditCard,
  Landmark,
  LayoutDashboard,
  LogOut,
  Menu,
  Package,
  Receipt,
  Scale,
  Search,
  Settings,
  ShoppingCart,
  Store,
  UserRound,
} from 'lucide-react'

import { logoutAction } from '@/features/auth/server/actions'
import { Avatar, AvatarFallback } from '@/shared/ui/avatar'
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
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/shared/ui/sheet'
import { NotificationsDropdown } from '@/widgets/app-shell/ui/notifications-dropdown'
import { LanguageSwitcher } from '@/widgets/app-shell/ui/language-switcher'
import type { NotificationMessage, UserSession } from '@/shared/types'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'

interface HeaderProps {
  notifications: NotificationMessage[]
  user: UserSession
  dict: Dictionary
  lang: Locale
}

export function Header({ notifications, user, dict, lang }: HeaderProps) {
  const pathname = usePathname()
  const title = getPageTitle(pathname, lang, dict)

  return (
    <header className="flex h-[72px] shrink-0 items-center gap-3 border-b border-border bg-background px-4 sm:px-6">
      <MobileNavigation dict={dict} lang={lang} pathname={pathname} />
      <div className="min-w-0 flex-1">
        <h1 className="truncate text-sm font-medium text-foreground">{title}</h1>
      </div>

      <button
        type="button"
        className="hidden h-10 w-[min(36vw,500px)] items-center gap-2 rounded-md border border-border bg-background px-3 text-sm text-muted-foreground transition hover:border-foreground/25 lg:flex"
        aria-label={dict.header.searchPlaceholder}
      >
        <Search className="size-4 shrink-0" />
        <span className="truncate">{dict.header.searchPlaceholder}</span>
        <kbd className="ml-auto rounded border border-border bg-muted px-1.5 py-0.5 font-mono text-[10px]">
          Ctrl K
        </kbd>
      </button>

      <div className="flex items-center gap-1">
        <HeaderAction><LanguageSwitcher dict={dict} lang={lang} /></HeaderAction>
        <HeaderAction>
          <NotificationsDropdown notifications={notifications} dict={dict} lang={lang} />
        </HeaderAction>
        <ProfileDropdown user={user} dict={dict} lang={lang} />
      </div>
    </header>
  )
}

function getPageTitle(pathname: string, lang: Locale, dict: Dictionary) {
  const titles: Record<string, string> = {
    dashboard: dict.nav.dashboard,
    lancamentos: dict.nav.lancamentos,
    despesas: dict.nav.despesas,
    estoque: dict.nav.estoque,
    extrato: dict.nav.extrato,
    dre: dict.nav.dre,
    balanco: dict.nav.balanco,
    conectores: dict.nav.conectores,
    notificacoes: dict.nav.notificacoes,
    contador: dict.nav.contador,
    configuracoes: dict.nav.configuracoes,
    plano: dict.nav.plano,
    bpo: dict.nav.bpo,
  }
  const segment = pathname.replace(`/${lang}/`, '').split('/')[0]
  return titles[segment] ?? dict.header.fallbackTitle
}

function HeaderAction({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex size-9 items-center justify-center rounded-md text-muted-foreground transition hover:bg-muted hover:text-foreground">
      {children}
    </div>
  )
}

function MobileNavigation({ dict, lang, pathname }: { dict: Dictionary; lang: Locale; pathname: string }) {
  const items = [
    ['dashboard', dict.nav.dashboard, LayoutDashboard],
    ['lancamentos', dict.nav.lancamentos, ShoppingCart],
    ['despesas', dict.nav.despesas, Receipt],
    ['estoque', dict.nav.estoque, Package],
    ['extrato', dict.nav.extrato, Landmark],
    ['dre', dict.nav.dre, BarChart3],
    ['balanco', dict.nav.balanco, Scale],
    ['conectores', dict.nav.conectores, Store],
    ['notificacoes', dict.nav.notificacoes, Bell],
    ['contador', dict.nav.contador, Calculator],
    ['configuracoes', dict.nav.configuracoes, Settings],
  ] as const

  return (
    <Sheet>
      <SheetTrigger render={<Button variant="outline" size="icon" className="lg:hidden" aria-label="Abrir navegação" />}>
        <Menu />
      </SheetTrigger>
      <SheetContent side="left" className="w-[286px] sm:max-w-[286px]">
        <SheetHeader className="border-b">
          <SheetTitle className="flex items-center gap-2">
            <span className="flex size-7 items-center justify-center rounded-md bg-foreground text-xs font-bold text-background">B</span>
            Brasaller
          </SheetTitle>
        </SheetHeader>
        <nav className="flex flex-col gap-1 px-3">
          {items.map(([href, label, Icon]) => {
            const fullHref = `/${lang}/${href}`
            const active = pathname === fullHref || pathname.startsWith(`${fullHref}/`)
            return (
              <Link
                key={href}
                href={fullHref}
                className={active
                  ? 'flex items-center gap-3 rounded-md bg-muted px-3 py-2.5 text-sm font-medium text-foreground'
                  : 'flex items-center gap-3 rounded-md px-3 py-2.5 text-sm text-muted-foreground hover:bg-muted hover:text-foreground'}
              >
                <Icon className="size-4" />
                {label}
              </Link>
            )
          })}
        </nav>
      </SheetContent>
    </Sheet>
  )
}

function ProfileDropdown({ user, dict, lang }: { user: UserSession; dict: Dictionary; lang: Locale }) {
  const displayName = user.fullName || user.email
  const initials = getInitials(displayName)

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        render={
          <Button
            type="button"
            variant="outline"
            className="h-10 gap-2 rounded-md border-border bg-background px-2 pr-3 text-foreground shadow-none hover:border-foreground/25 hover:bg-muted"
            aria-label="Abrir menu do perfil"
          >
            <Avatar size="sm">
              <AvatarFallback className="bg-foreground text-xs font-semibold text-background">{initials}</AvatarFallback>
            </Avatar>
            <span className="hidden max-w-36 truncate text-sm font-medium md:inline">{displayName}</span>
            <ChevronDown className="size-4 text-muted-foreground" />
          </Button>
        }
      />
      <DropdownMenuContent align="end" sideOffset={8} className="w-72 rounded-lg border-border bg-popover p-2 shadow-lg">
        <DropdownMenuGroup>
          <DropdownMenuLabel className="rounded-md p-3">
            <div className="flex items-center gap-3">
              <Avatar>
                <AvatarFallback className="bg-foreground text-sm font-semibold text-background">{initials}</AvatarFallback>
              </Avatar>
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-foreground">{displayName}</p>
                <p className="truncate text-xs text-muted-foreground">{user.email}</p>
              </div>
            </div>
          </DropdownMenuLabel>
        </DropdownMenuGroup>
        <DropdownMenuSeparator />
        <DropdownMenuGroup>
          <ProfileLink href={`/${lang}/configuracoes`} icon={UserRound}>{dict.header.profileMenu.profileSecurity}</ProfileLink>
          <ProfileLink href={`/${lang}/configuracoes`} icon={Building2}>{dict.header.profileMenu.fiscalData}</ProfileLink>
          <ProfileLink href={`/${lang}/plano`} icon={CreditCard}>{dict.header.profileMenu.plan}</ProfileLink>
          <ProfileLink href={`/${lang}/configuracoes`} icon={Settings}>{dict.header.profileMenu.settings}</ProfileLink>
        </DropdownMenuGroup>
        <DropdownMenuSeparator />
        <DropdownMenuGroup>
          <form action={logoutAction}>
            <DropdownMenuItem variant="destructive" nativeButton render={<button type="submit" className="w-full" />}>
              <LogOut />
              {dict.header.profileMenu.signOut}
            </DropdownMenuItem>
          </form>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

function ProfileLink({
  href,
  icon: Icon,
  children,
}: {
  href: string
  icon: React.ComponentType<{ className?: string }>
  children: React.ReactNode
}) {
  return (
    <DropdownMenuItem render={<Link href={href} />} className="rounded-md">
      <Icon className="size-4 text-muted-foreground" />
      {children}
    </DropdownMenuItem>
  )
}

function getInitials(value: string) {
  return value
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase() || 'B'
}
