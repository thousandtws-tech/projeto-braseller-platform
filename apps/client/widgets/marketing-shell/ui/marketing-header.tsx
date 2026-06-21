'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { Menu } from 'lucide-react'

import { cn } from '@/shared/lib/utils'
import { Button } from '@/shared/ui/button'
import { LanguageSwitcher } from '@/widgets/app-shell/ui/language-switcher'
import { ThemeToggle } from '@/widgets/app-shell/ui/theme-toggle'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'

interface MarketingHeaderProps {
  dict: Dictionary
  lang: Locale
}

export function MarketingHeader({ dict, lang }: MarketingHeaderProps) {
  const pathname = usePathname()
  const nav = dict.marketing.nav

  const navItems = [
    { href: 'inicio', label: nav.inicio },
    { href: 'sobre', label: nav.sobre },
    { href: 'planos', label: nav.planos },
    { href: 'contato', label: nav.contato },
  ]

  return (
    <header className="sticky top-0 z-40 flex h-16 shrink-0 items-center gap-4 border-b border-slate-200 bg-white/95 px-6 backdrop-blur supports-[backdrop-filter]:bg-white/80">
      <Link href={`/${lang}/inicio`} className="flex items-center gap-3 shrink-0">
        <div className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-blue-600 shadow-sm">
          <span className="text-sm font-bold text-white">B</span>
        </div>
        <span className="text-sm font-semibold text-slate-900">Brasaller</span>
      </Link>

      <nav className="hidden flex-1 items-center justify-center gap-1 md:flex">
        {navItems.map(({ href, label }) => {
          const fullHref = `/${lang}/${href}`
          const active = pathname === fullHref || pathname.startsWith(`${fullHref}/`)

          return (
            <Link
              key={href}
              href={fullHref}
              className={cn(
                'rounded-lg px-3 py-2 text-sm transition-colors',
                active
                  ? 'font-medium text-blue-700'
                  : 'text-slate-600 hover:text-slate-900'
              )}
            >
              {label}
            </Link>
          )
        })}
      </nav>

      <div className="flex flex-1 items-center justify-end gap-1.5 md:flex-none">
        <div className="hidden items-center gap-1.5 md:flex">
          <ThemeToggle dict={dict} />
          <LanguageSwitcher dict={dict} lang={lang} />
        </div>

        <Button variant="ghost" size="sm" render={<Link href={`/${lang}/login`} />}>
          {nav.login}
        </Button>
        <Button size="sm" render={<Link href={`/${lang}/register`} />}>
          {nav.signup}
        </Button>

        <Button variant="ghost" size="icon" className="md:hidden" aria-label={nav.inicio}>
          <Menu className="size-4" />
        </Button>
      </div>
    </header>
  )
}
