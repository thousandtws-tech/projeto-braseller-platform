'use client'

import { useEffect, useState } from 'react'
import { useTheme } from 'next-themes'
import { Check, Monitor, Moon, Sun } from 'lucide-react'

import { Button } from '@/shared/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/shared/ui/dropdown-menu'

import type { Dictionary } from '@/shared/i18n/get-dictionary'

interface ThemeToggleProps {
  dict: Dictionary
}

const OPTIONS = [
  { value: 'light', icon: Sun },
  { value: 'dark', icon: Moon },
  { value: 'system', icon: Monitor },
] as const

export function ThemeToggle({ dict }: ThemeToggleProps) {
  const { theme, setTheme } = useTheme()
  const [mounted, setMounted] = useState(false)
  const t = dict.header.theme

  useEffect(() => {
    setMounted(true)
  }, [])

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
            aria-label={t.ariaLabel}
          >
            {mounted && theme === 'dark' ? (
              <Moon className="size-4" />
            ) : mounted && theme === 'light' ? (
              <Sun className="size-4" />
            ) : (
              <Monitor className="size-4" />
            )}
          </Button>
        }
      />

      <DropdownMenuContent
        align="end"
        sideOffset={8}
        className="
          w-44
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
            {t.ariaLabel}
          </p>
        </div>

        {OPTIONS.map(({ value, icon: Icon }) => {
          const active = mounted && theme === value

          return (
            <DropdownMenuItem
              key={value}
              onClick={() => setTheme(value)}
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
              <span className="flex items-center gap-2">
                <Icon className="size-4 text-slate-500" />
                <span className={active ? 'font-semibold text-slate-900' : 'text-slate-600'}>
                  {t[value]}
                </span>
              </span>

              {active && <Check className="size-4 text-blue-600" />}
            </DropdownMenuItem>
          )
        })}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
