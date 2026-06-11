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
    <header className="flex h-16 shrink-0 items-center gap-4 border-b border-slate-200 bg-white/95 px-6 backdrop-blur supports-[backdrop-filter]:bg-white/80">
      <div className="min-w-0 flex-1">
        <h1 className="truncate text-sm font-semibold text-slate-900">
          {title}
        </h1>
      </div>

      <button
        type="button"
        className="hidden h-10 w-72 items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 text-sm text-slate-500 transition hover:border-slate-300 hover:bg-white hover:shadow-sm lg:flex"
        aria-label={dict.header.searchPlaceholder}
      >
        <Search className="size-4 shrink-0 text-slate-400" />
        <span className="truncate text-sm">{dict.header.searchPlaceholder}</span>
        <kbd className="ml-auto rounded-md border border-slate-200 bg-white px-1.5 py-0.5 font-mono text-[11px] text-slate-500 shadow-sm">
          ⌘K
        </kbd>
      </button>

      <div className="flex items-center gap-1.5">
        <HeaderAction>
          <LanguageSwitcher dict={dict} lang={lang} />
        </HeaderAction>

        <HeaderAction>
          <NotificationsDropdown notifications={notifications} dict={dict} lang={lang} />
        </HeaderAction>

        <ProfileDropdown user={user} dict={dict} lang={lang} />
      </div>
    </header>
  )
}

function HeaderAction({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex size-10 items-center justify-center rounded-xl text-slate-600 transition hover:bg-slate-50">
      {children}
    </div>
  )
}

function ProfileDropdown({
  user,
  dict,
  lang,
}: {
  user: UserSession
  dict: Dictionary
  lang: Locale
}) {
  const displayName = user.fullName || user.email
  const initials = getInitials(displayName)

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        render={
          <Button
            type="button"
            variant="outline"
            className="h-10 gap-2 rounded-xl border-slate-200 bg-slate-50 px-2 pr-3 text-slate-700 shadow-none transition hover:border-slate-300 hover:bg-white hover:shadow-sm"
            aria-label="Abrir menu do perfil"
          >
            <Avatar size="sm">
              <AvatarFallback className="bg-blue-100 text-xs font-semibold text-blue-700">
                {initials}
              </AvatarFallback>
            </Avatar>

            <span className="hidden max-w-40 truncate text-sm font-medium md:inline">
              {displayName}
            </span>

            <ChevronDown className="size-4 text-slate-400" />
          </Button>
        }
      />

      <DropdownMenuContent
        align="end"
        sideOffset={8}
        className="w-72 rounded-2xl border-slate-200 bg-white p-2 shadow-xl shadow-slate-200/60"
      >
        <DropdownMenuGroup>
          <DropdownMenuLabel className="rounded-xl p-3">
            <div className="flex items-center gap-3">
              <Avatar>
                <AvatarFallback className="bg-blue-100 text-sm font-semibold text-blue-700">
                  {initials}
                </AvatarFallback>
              </Avatar>

              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-slate-900">
                  {displayName}
                </p>
                <p className="truncate text-xs text-slate-500">
                  {user.email}
                </p>
              </div>
            </div>
          </DropdownMenuLabel>
        </DropdownMenuGroup>

        <DropdownMenuSeparator className="my-1 bg-slate-100" />

        <DropdownMenuGroup>
          <DropdownMenuItem
            render={<Link href={`/${lang}/configuracoes`} />}
            className="rounded-xl px-3 py-2.5 text-sm text-slate-700 focus:bg-slate-50 focus:text-slate-900"
          >
            <UserRound className="size-4 text-slate-500" />
            {dict.header.profileMenu.profileSecurity}
          </DropdownMenuItem>

          <DropdownMenuItem
            render={<Link href={`/${lang}/configuracoes`} />}
            className="rounded-xl px-3 py-2.5 text-sm text-slate-700 focus:bg-slate-50 focus:text-slate-900"
          >
            <Building2 className="size-4 text-slate-500" />
            {dict.header.profileMenu.fiscalData}
          </DropdownMenuItem>

          <DropdownMenuItem
            render={<Link href={`/${lang}/plano`} />}
            className="rounded-xl px-3 py-2.5 text-sm text-slate-700 focus:bg-slate-50 focus:text-slate-900"
          >
            <CreditCard className="size-4 text-slate-500" />
            {dict.header.profileMenu.plan}
          </DropdownMenuItem>

          <DropdownMenuItem
            render={<Link href={`/${lang}/configuracoes`} />}
            className="rounded-xl px-3 py-2.5 text-sm text-slate-700 focus:bg-slate-50 focus:text-slate-900"
          >
            <Settings className="size-4 text-slate-500" />
            {dict.header.profileMenu.settings}
          </DropdownMenuItem>
        </DropdownMenuGroup>

        <DropdownMenuSeparator className="my-1 bg-slate-100" />

        <DropdownMenuGroup>
          <form action={logoutAction}>
            <DropdownMenuItem
              variant="destructive"
              nativeButton
              render={<button type="submit" className="w-full" />}
              className="rounded-xl px-3 py-2.5 text-sm text-red-600 focus:bg-red-50 focus:text-red-700"
            >
              <LogOut className="size-4 text-red-500" />
              {dict.header.profileMenu.signOut}
            </DropdownMenuItem>
          </form>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

function getInitials(value: string) {
  return (
    value
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0])
      .join('')
      .toUpperCase() || 'B'
  )
}