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
import { logoutAction } from '@/app/actions/auth'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { NotificationsDropdown } from '@/components/shadcn-space/dropdown-menu/dropdown-menu-02'
import type { NotificationMessage, UserSession } from '@/types'

const BREADCRUMBS: Record<string, string> = {
  '/dashboard': 'Dashboard',
  '/lancamentos': 'Lançamentos',
  '/despesas': 'Despesas',
  '/dre': 'DRE',
  '/conectores': 'Conectores',
  '/notificacoes': 'Notificações',
  '/configuracoes': 'Configurações',
  '/plano': 'Plano',
}

interface HeaderProps {
  notifications: NotificationMessage[]
  user: UserSession
}

export function Header({ notifications, user }: HeaderProps) {
  const pathname = usePathname()
  const title = BREADCRUMBS[pathname] ?? 'Brasaller'

  return (
    <header className="h-14 border-b border-border bg-card flex items-center px-6 gap-4 shrink-0">
      <div className="min-w-0 flex-1">
        <h1 className="truncate text-sm font-semibold text-foreground">{title}</h1>
      </div>

      <div className="hidden sm:flex items-center gap-2 h-8 w-56 rounded-lg border border-input bg-background px-3 text-sm text-muted-foreground cursor-text transition-colors hover:border-ring">
        <Search className="size-3.5 shrink-0" />
        <span className="text-xs">Buscar...</span>
        <kbd className="ml-auto rounded border border-border bg-muted px-1.5 py-0.5 font-mono text-xs">
          ⌘K
        </kbd>
      </div>

      <div className="flex items-center gap-2">
        <NotificationsDropdown notifications={notifications} />
        <ProfileDropdown user={user} />
      </div>
    </header>
  )
}

function ProfileDropdown({ user }: { user: UserSession }) {
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
          <DropdownMenuItem render={<Link href="/configuracoes" />}>
            <UserRound />
            Perfil e segurança
          </DropdownMenuItem>
          <DropdownMenuItem render={<Link href="/configuracoes" />}>
            <Building2 />
            Dados fiscais
          </DropdownMenuItem>
          <DropdownMenuItem render={<Link href="/plano" />}>
            <CreditCard />
            Plano
          </DropdownMenuItem>
          <DropdownMenuItem render={<Link href="/configuracoes" />}>
            <Settings />
            Configurações
          </DropdownMenuItem>
        </DropdownMenuGroup>
        <DropdownMenuSeparator />
        <DropdownMenuGroup>
          <form action={logoutAction}>
            <DropdownMenuItem variant="destructive" nativeButton={true} render={<button type="submit" className="w-full" />}>
              <LogOut />
              Sair
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
