'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import {
  Building2,
  ChevronDown,
  CreditCard,
  LogOut,
  Search,
  Settings,
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

  const breadcrumbs: Record<string, string> = {
    [`/${lang}/dashboard`]: dict.nav.dashboard,
    [`/${lang}/lancamentos`]: dict.nav.lancamentos,
    [`/${lang}/despesas`]: dict.nav.despesas,
    [`/${lang}/estoque`]: dict.nav.estoque,
    [`/${lang}/extrato`]: dict.nav.extrato,
    [`/${lang}/dre`]: dict.nav.dre,
    [`/${lang}/balanco`]: dict.nav.balanco,
    [`/${lang}/conectores`]: dict.nav.conectores,
    [`/${lang}/notificacoes`]: dict.nav.notificacoes,
    [`/${lang}/contador`]: dict.nav.contador,
    [`/${lang}/configuracoes`]: dict.nav.configuracoes,
    [`/${lang}/plano`]: dict.nav.plano,
    [`/${lang}/bpo`]: dict.nav.bpo,
  }

  const title = breadcrumbs[pathname] ?? dict.header.fallbackTitle

  return (
    <header className="h-14 border-b border-border bg-card flex items-center px-6 gap-4 shrink-0">
      <div className="min-w-0 flex-1">
        <h1 className="truncate text-sm font-semibold text-foreground">{title}</h1>
      </div>

      <div className="hidden sm:flex items-center gap-2 h-8 w-56 rounded-lg border border-input bg-background px-3 text-sm text-muted-foreground cursor-text transition-colors hover:border-ring">
        <Search className="size-3.5 shrink-0" />
        <span className="text-xs">{dict.header.searchPlaceholder}</span>
        <kbd className="ml-auto rounded border border-border bg-muted px-1.5 py-0.5 font-mono text-xs">
          ⌘K
        </kbd>
      </div>

      <div className="flex items-center gap-2">
        <LanguageSwitcher dict={dict} lang={lang} />
        <NotificationsDropdown notifications={notifications} dict={dict} lang={lang} />
        <ProfileDropdown user={user} dict={dict} lang={lang} />
      </div>
    </header>
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
            className="h-9 gap-2 px-2 pr-2.5"
            aria-label="Abrir menu do perfil"
          >
            <Avatar size="sm">
              <AvatarFallback className="bg-primary/10 text-primary">
                {initials}
              </AvatarFallback>
            </Avatar>
            <span className="hidden max-w-32 truncate text-xs font-medium md:inline">
              {displayName}
            </span>
            <ChevronDown data-icon="inline-end" />
          </Button>
        }
      />
      <DropdownMenuContent align="end" className="w-64 p-1.5">
        <DropdownMenuGroup>
          <DropdownMenuLabel className="p-3">
            <div className="flex items-center gap-3">
              <Avatar>
                <AvatarFallback className="bg-primary/10 text-primary">
                  {initials}
                </AvatarFallback>
              </Avatar>
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-popover-foreground">
                  {displayName}
                </p>
                <p className="truncate text-xs text-muted-foreground">{user.email}</p>
              </div>
            </div>
          </DropdownMenuLabel>
        </DropdownMenuGroup>
        <DropdownMenuSeparator />
        <DropdownMenuGroup>
          <DropdownMenuItem render={<Link href={`/${lang}/configuracoes`} />}>
            <UserRound />
            {dict.header.profileMenu.profileSecurity}
          </DropdownMenuItem>
          <DropdownMenuItem render={<Link href={`/${lang}/configuracoes`} />}>
            <Building2 />
            {dict.header.profileMenu.fiscalData}
          </DropdownMenuItem>
          <DropdownMenuItem render={<Link href={`/${lang}/plano`} />}>
            <CreditCard />
            {dict.header.profileMenu.plan}
          </DropdownMenuItem>
          <DropdownMenuItem render={<Link href={`/${lang}/configuracoes`} />}>
            <Settings />
            {dict.header.profileMenu.settings}
          </DropdownMenuItem>
        </DropdownMenuGroup>
        <DropdownMenuSeparator />
        <DropdownMenuGroup>
          <form action={logoutAction}>
            <DropdownMenuItem variant="destructive" nativeButton={true} render={<button type="submit" className="w-full" />}>
              <LogOut />
              {dict.header.profileMenu.signOut}
            </DropdownMenuItem>
          </form>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
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
