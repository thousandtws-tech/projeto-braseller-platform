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

const CATEGORIES = [
  { value: 'OPERATIONAL', label: 'Operacional' },
  { value: 'PACKAGING', label: 'Embalagens' },
  { value: 'SUPPLIES', label: 'Suprimentos' },
  { value: 'LABOR', label: 'Mão de obra' },
  { value: 'BANK_FEE', label: 'Tarifas bancárias' },
  { value: 'SHIPPING', label: 'Frete' },
  { value: 'TAX', label: 'Impostos' },
  { value: 'OTHER', label: 'Outros' },
]

interface ExpenseFiltersProps {
  from: string
  to: string
  category: string
}

export function ExpenseFilters({ from, to, category }: ExpenseFiltersProps) {
  const router = useRouter()

  function navigate(newFrom: string, newTo: string, cat?: string) {
    const sp = new URLSearchParams({ from: newFrom, to: newTo })
    if (cat) sp.set('category', cat)
    router.push(`/despesas?${sp}`)
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

  const label = new Intl.DateTimeFormat('pt-BR', {
    month: 'long',
    year: 'numeric',
  }).format(fromDate)

  return (
    <div className="flex flex-wrap items-center gap-2">
      {/* Month navigation */}
      <div className="flex items-center gap-0.5 rounded-lg border border-border bg-background p-0.5">
        <Button variant="ghost" size="icon-sm" onClick={prevMonth} aria-label="Mês anterior">
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
          aria-label="Próximo mês"
        >
          <ChevronRight className="size-3.5" />
        </Button>
      </div>

      {/* Jump to current month */}
      {!isCurrentMonth && (
        <Button variant="outline" size="sm" onClick={goToCurrentMonth} className="text-xs">
          Mês atual
        </Button>
      )}

      {/* Category filter */}
      <Select
        value={category || ''}
        onValueChange={(v) => navigate(from, to, (v as string) || undefined)}
      >
        <SelectTrigger className="h-8 min-w-44 gap-1.5 text-xs" size="sm">
          <SlidersHorizontal className="size-3 text-muted-foreground" />
          <SelectValue placeholder="Todas as categorias" />
        </SelectTrigger>
        <SelectContent align="start">
          <SelectGroup>
            <SelectItem value="">Todas as categorias</SelectItem>
            {CATEGORIES.map((c) => (
              <SelectItem key={c.value} value={c.value}>
                {c.label}
              </SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>
    </div>
  )
}
