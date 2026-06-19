'use client'

import { usePathname, useRouter } from 'next/navigation'
import { Check, Globe } from 'lucide-react'

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

export function LanguageSwitcher({
  dict,
  lang,
}: LanguageSwitcherProps) {
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
          <Button
            type="button"
            variant="ghost"
            className="
              h-10 w-10 rounded-xl
              border border-transparent
              bg-transparent
              text-slate-600
              transition-all
              hover:bg-slate-50
              hover:text-slate-900
              hover:border-slate-200
            "
            aria-label={dict.languages[lang]}
          >
            <Globe className="size-4" />
          </Button>
        }
      />

      <DropdownMenuContent
        align="end"
        sideOffset={8}
        className="
          w-52
          rounded-2xl
          border-slate-200
          bg-white
          p-2
          shadow-xl
          shadow-slate-200/60
        "
      >
        <div className="px-3 py-2">
          <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
            Language
          </p>
        </div>

        {locales.map((locale) => {
          const active = locale === lang

          return (
            <DropdownMenuItem
              key={locale}
              onClick={() => switchTo(locale)}
              className="
                flex items-center justify-between
                rounded-xl
                px-3
                py-2.5
                text-sm
                transition-colors
                focus:bg-slate-50
              "
            >
              <span
                className={
                  active
                    ? 'font-semibold text-slate-900'
                    : 'text-slate-600'
                }
              >
                {dict.languages[locale]}
              </span>

              {active && (
                <Check className="size-4 text-blue-600" />
              )}
            </DropdownMenuItem>
          )
        })}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}