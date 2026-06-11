'use client'

import { usePathname, useRouter } from 'next/navigation'
import { Globe } from 'lucide-react'
import { Button } from '@/shared/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/shared/ui/dropdown-menu'
import { locales, type Locale } from '@/shared/i18n/config'
import type { Dictionary } from '@/shared/i18n/get-dictionary'

interface LanguageSwitcherProps {
  dict: Dictionary
  lang: Locale
}

export function LanguageSwitcher({ dict, lang }: LanguageSwitcherProps) {
  const pathname = usePathname()
  const router = useRouter()

  function switchTo(locale: Locale) {
    const segments = pathname.split('/')
    segments[1] = locale
    router.push(segments.join('/') || '/')
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        render={
          <Button type="button" variant="ghost" size="icon-sm" aria-label={dict.languages[lang]}>
            <Globe />
          </Button>
        }
      />
      <DropdownMenuContent align="end" className="w-44">
        {locales.map((locale) => (
          <DropdownMenuItem
            key={locale}
            onClick={() => switchTo(locale)}
            className={locale === lang ? 'font-semibold' : undefined}
          >
            {dict.languages[locale]}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
