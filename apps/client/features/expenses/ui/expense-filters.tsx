'use client'

import { useRouter } from 'next/navigation'
import { ChevronLeft, ChevronRight, SlidersHorizontal } from 'lucide-react'
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

const LOCALE_MAP: Record<Locale, string> = { 'pt-BR': 'pt-BR', en: 'en-US', es: 'es-ES' }

const CATEGORY_VALUES = ['OPERATIONAL', 'PACKAGING', 'SUPPLIES', 'LABOR', 'BANK_FEE', 'SHIPPING', 'TAX', 'OTHER'] as const

interface ExpenseFiltersProps {
  from: string
  to: string
  category: string
  dict: Dictionary
  lang: Locale
}

export function ExpenseFilters({ from, to, category, dict, lang }: ExpenseFiltersProps) {
  const router = useRouter()

  function navigate(newFrom: string, newTo: string, cat?: string) {
    const sp = new URLSearchParams({ from: newFrom, to: newTo })
    if (cat) sp.set('category', cat)
    router.push(`/${lang}/despesas?${sp}`)
  }

  function prevMonth() {
    const d = new Date(from + 'T00:00:00')
    const m = d.getMonth() - 1
    const y = d.getFullYear()
    const newFrom = new Date(y, m, 1).toISOString().split('T')[0]
    const newTo = new Date(y, m + 1, 0).toISOString().split('T')[0]
    navigate(newFrom, newTo, category || undefined)
  }

  function nextMonth() {
    const d = new Date(from + 'T00:00:00')
    const m = d.getMonth() + 1
    const y = d.getFullYear()
    const newFrom = new Date(y, m, 1).toISOString().split('T')[0]
    const newTo = new Date(y, m + 1, 0).toISOString().split('T')[0]
    navigate(newFrom, newTo, category || undefined)
  }

  function goToCurrentMonth() {
    const now = new Date()
    const y = now.getFullYear()
    const m = now.getMonth()
    const newFrom = new Date(y, m, 1).toISOString().split('T')[0]
    const newTo = new Date(y, m + 1, 0).toISOString().split('T')[0]
    navigate(newFrom, newTo, category || undefined)
  }

  const fromDate = new Date(from + 'T00:00:00')
  const now = new Date()
  const isCurrentMonth =
    fromDate.getFullYear() === now.getFullYear() &&
    fromDate.getMonth() === now.getMonth()
  const isAfterCurrentMonth =
    fromDate.getFullYear() > now.getFullYear() ||
    (fromDate.getFullYear() === now.getFullYear() && fromDate.getMonth() > now.getMonth())

  const label = new Intl.DateTimeFormat(LOCALE_MAP[lang], {
    month: 'long',
    year: 'numeric',
  }).format(fromDate)

  return (
    <div className="flex flex-wrap items-center gap-2">
      {/* Month navigation */}
      <div className="flex items-center gap-0.5 rounded-lg border border-border bg-background p-0.5">
        <Button variant="ghost" size="icon-sm" onClick={prevMonth} aria-label={dict.expenses.filters.previousMonth}>
          <ChevronLeft className="size-3.5" />
        </Button>
        <span className="min-w-40 px-2 text-center text-sm font-medium capitalize">
          {label}
        </span>
        <Button
          variant="ghost"
          size="icon-sm"
          onClick={nextMonth}
          disabled={isCurrentMonth || isAfterCurrentMonth}
          aria-label={dict.expenses.filters.nextMonth}
        >
          <ChevronRight className="size-3.5" />
        </Button>
      </div>

      {/* Jump to current month */}
      {!isCurrentMonth && (
        <Button variant="outline" size="sm" onClick={goToCurrentMonth} className="text-xs">
          {dict.expenses.filters.currentMonth}
        </Button>
      )}

      {/* Category filter */}
      <Select
        value={category || ''}
        onValueChange={(v) => navigate(from, to, (v as string) || undefined)}
      >
        <SelectTrigger className="h-8 min-w-44 gap-1.5 text-xs" size="sm">
          <SlidersHorizontal className="size-3 text-muted-foreground" />
          <SelectValue placeholder={dict.expenses.filters.allCategories} />
        </SelectTrigger>
        <SelectContent align="start">
          <SelectGroup>
            <SelectItem value="">{dict.expenses.filters.allCategories}</SelectItem>
            {CATEGORY_VALUES.map((c) => (
              <SelectItem key={c} value={c}>
                {dict.dre.categories[c]}
              </SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>
    </div>
  )
}
