'use client'

import { useRouter } from 'next/navigation'
import {
  ChevronLeft,
  ChevronRight,
  SlidersHorizontal,
  CalendarDays,
} from 'lucide-react'

import { Button } from '@/shared/ui/button'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/ui/select'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'

const LOCALE_MAP: Record<Locale, string> = {
  'pt-BR': 'pt-BR',
  en: 'en-US',
  es: 'es-ES',
}

const CATEGORY_VALUES = [
  'OPERATIONAL',
  'PACKAGING',
  'SUPPLIES',
  'LABOR',
  'BANK_FEE',
  'SHIPPING',
  'TAX',
  'OTHER',
] as const

interface ExpenseFiltersProps {
  from: string
  to: string
  category: string
  dict: Dictionary
  lang: Locale
}

export function ExpenseFilters({
  from,
  to,
  category,
  dict,
  lang,
}: ExpenseFiltersProps) {
  const router = useRouter()

  function navigate(newFrom: string, newTo: string, cat?: string) {
    const searchParams = new URLSearchParams({
      from: newFrom,
      to: newTo,
    })

    if (cat) {
      searchParams.set('category', cat)
    }

    router.push(`/${lang}/despesas?${searchParams}`)
  }

  function prevMonth() {
    const date = new Date(`${from}T00:00:00`)
    const month = date.getMonth() - 1
    const year = date.getFullYear()

    const newFrom = new Date(year, month, 1).toISOString().split('T')[0]
    const newTo = new Date(year, month + 1, 0).toISOString().split('T')[0]

    navigate(newFrom, newTo, category || undefined)
  }

  function nextMonth() {
    const date = new Date(`${from}T00:00:00`)
    const month = date.getMonth() + 1
    const year = date.getFullYear()

    const newFrom = new Date(year, month, 1).toISOString().split('T')[0]
    const newTo = new Date(year, month + 1, 0).toISOString().split('T')[0]

    navigate(newFrom, newTo, category || undefined)
  }

  function goToCurrentMonth() {
    const now = new Date()
    const year = now.getFullYear()
    const month = now.getMonth()

    const newFrom = new Date(year, month, 1).toISOString().split('T')[0]
    const newTo = new Date(year, month + 1, 0).toISOString().split('T')[0]

    navigate(newFrom, newTo, category || undefined)
  }

  const fromDate = new Date(`${from}T00:00:00`)
  const now = new Date()

  const isCurrentMonth =
    fromDate.getFullYear() === now.getFullYear() &&
    fromDate.getMonth() === now.getMonth()

  const isAfterCurrentMonth =
    fromDate.getFullYear() > now.getFullYear() ||
    (fromDate.getFullYear() === now.getFullYear() &&
      fromDate.getMonth() > now.getMonth())

  const label = new Intl.DateTimeFormat(LOCALE_MAP[lang], {
    month: 'long',
    year: 'numeric',
  }).format(fromDate)

  return (
    <div className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-3 shadow-sm shadow-slate-200/40 sm:flex-row sm:flex-wrap sm:items-center sm:justify-between">
      <div className="flex flex-wrap items-center gap-2">
        <div className="flex h-10 items-center gap-1 rounded-xl border border-slate-200 bg-slate-50 p-1">
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            onClick={prevMonth}
            aria-label={dict.expenses.filters.previousMonth}
            className="size-8 rounded-lg text-slate-500 hover:bg-white hover:text-slate-900 hover:shadow-sm"
          >
            <ChevronLeft className="size-4" />
          </Button>

          <div className="flex min-w-44 items-center justify-center gap-2 px-2 text-sm font-medium capitalize text-slate-900">
            <CalendarDays className="size-4 text-slate-400" />
            {label}
          </div>

          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            onClick={nextMonth}
            disabled={isCurrentMonth || isAfterCurrentMonth}
            aria-label={dict.expenses.filters.nextMonth}
            className="size-8 rounded-lg text-slate-500 hover:bg-white hover:text-slate-900 hover:shadow-sm disabled:cursor-not-allowed disabled:opacity-40"
          >
            <ChevronRight className="size-4" />
          </Button>
        </div>

        {!isCurrentMonth && (
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={goToCurrentMonth}
            className="h-10 rounded-xl border-slate-200 bg-white px-4 text-xs font-medium text-slate-700 shadow-sm transition hover:bg-slate-50"
          >
            {dict.expenses.filters.currentMonth}
          </Button>
        )}
      </div>

      <Select
        value={category || ''}
        onValueChange={(value) => navigate(from, to, value || undefined)}
      >
        <SelectTrigger
          size="sm"
          className="h-10 min-w-52 rounded-xl border-slate-200 bg-white px-3 text-sm text-slate-700 shadow-sm hover:bg-slate-50"
        >
          <div className="flex items-center gap-2">
            <SlidersHorizontal className="size-4 text-slate-400" />
            <SelectValue placeholder={dict.expenses.filters.allCategories} />
          </div>
        </SelectTrigger>

        <SelectContent
          align="end"
          className="rounded-2xl border-slate-200 bg-white p-2 shadow-xl shadow-slate-200/60"
        >
          <SelectGroup>
            <SelectItem
              value=""
              className="rounded-xl px-3 py-2.5 text-sm text-slate-700 focus:bg-slate-50"
            >
              {dict.expenses.filters.allCategories}
            </SelectItem>

            {CATEGORY_VALUES.map((categoryValue) => (
              <SelectItem
                key={categoryValue}
                value={categoryValue}
                className="rounded-xl px-3 py-2.5 text-sm text-slate-700 focus:bg-slate-50"
              >
                {dict.dre.categories[categoryValue]}
              </SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>
    </div>
  )
}