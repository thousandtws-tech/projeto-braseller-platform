import { Suspense } from 'react'
import { TrendingUp, ShoppingCart, DollarSign, ArrowDownLeft } from 'lucide-react'

import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Badge } from '@/shared/ui/badge'
import { Skeleton } from '@/shared/ui/skeleton'
import { DatePicker } from '@/shared/ui/date-picker'
import { getToken, getSession } from '@/entities/session/server/session'
import {
  getDashboard,
  getReportEntries,
  getReportsSummary,
  formatCurrency,
  formatDate,
} from '@/shared/api/gateway'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'
import type {
  DashboardView,
  PlatformBreakdown,
  ReportEntry,
  ReportsSummary,
} from '@/shared/types'

const LOCALE_MAP: Record<Locale, string> = {
  'pt-BR': 'pt-BR',
  en: 'en-US',
  es: 'es-ES',
}

function getCurrentPeriod(lang: Locale) {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth()

  const from = `${year}-${String(month + 1).padStart(2, '0')}-01`
  const lastDay = new Date(year, month + 1, 0).getDate()
  const to = `${year}-${String(month + 1).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`

  const label = new Date(year, month, 1).toLocaleDateString(LOCALE_MAP[lang], {
    month: 'long',
    year: 'numeric',
  })

  return { from, to, label }
}

interface PageProps {
  params: Promise<{ lang: Locale }>
}

export async function generateMetadata({ params }: PageProps) {
  const { lang } = await params
  const dict = await getDictionary(lang)

  return {
    title: dict.dashboard.title,
  }
}

export default async function DashboardPage({ params }: PageProps) {
  const { lang } = await params
  const token = (await getToken()) ?? ''
  const session = await getSession()
  const tenantId = session?.tenantId
  const dict = await getDictionary(lang)
  const period = getCurrentPeriod(lang)

  // Prefetch data for all sections to avoid waterfall
  const [summary, dashboardData, entries] = tenantId ? await Promise.all([
    getReportsSummary(token, tenantId, { from: period.from, to: period.to }),
    getDashboard(token, tenantId),
    getReportEntries(token, tenantId)
  ]) : [null, null, []]

  return (
    <div className="mx-auto max-w-7xl space-y-6 px-6 py-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-semibold tracking-tight text-slate-900">
            {dict.dashboard.title}
          </h2>
          <p className="mt-1 text-sm capitalize text-slate-500">
            {period.label}
          </p>
        </div>

        <PeriodSelector dict={dict} period={period} />
      </div>

      <KpiCards summary={summary} dict={dict} />

      <ChartsSection 
        dashboardData={dashboardData} 
        entries={entries} 
        dict={dict} 
      />

      <RecentOrdersTable 
        orders={dashboardData?.recentOrders ?? []} 
        dict={dict} 
        lang={lang} 
      />
    </div>
  )
}

function PeriodSelector({
  dict,
  period,
}: {
  dict: Dictionary
  period: { from: string }
}) {
  return (
    <DatePicker
      name="period"
      defaultValue={period.from}
      displayFormat="LLLL yyyy"
      placeholder={dict.dashboard.selectPeriod}
      buttonClassName="h-10 w-44 rounded-xl border-slate-200 bg-white text-sm capitalize shadow-sm hover:bg-slate-50"
    />
  )
}

function KpiCards({
  summary,
  dict,
}: {
  summary: ReportsSummary | null
  dict: Dictionary
}) {
  const cards = [
    {
      title: dict.dashboard.kpis.grossRevenue,
      value: formatCurrency(summary?.gross_value ?? 0),
      icon: DollarSign,
      color: 'text-blue-600',
      bg: 'bg-blue-50',
    },
    {
      title: dict.dashboard.kpis.received,
      value: formatCurrency(summary?.received_value ?? 0),
      icon: TrendingUp,
      color: 'text-emerald-600',
      bg: 'bg-emerald-50',
    },
    {
      title: dict.dashboard.kpis.feesAndShipping,
      value: formatCurrency(summary?.fee_value ?? 0),
      icon: ArrowDownLeft,
      color: 'text-red-600',
      bg: 'bg-red-50',
    },
    {
      title: dict.dashboard.kpis.totalOrders,
      value: String(summary?.entry_count ?? 0),
      icon: ShoppingCart,
      color: 'text-amber-600',
      bg: 'bg-amber-50',
    },
  ]

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {cards.map((card) => (
        <Card
          key={card.title}
          className="rounded-2xl border-slate-200 bg-white shadow-sm shadow-slate-200/40 transition hover:-translate-y-0.5 hover:shadow-md"
        >
          <CardContent className="p-5">
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <p className="truncate text-sm font-medium text-slate-500">
                  {card.title}
                </p>
                <p className="mt-2 text-2xl font-semibold tracking-tight text-slate-900">
                  {card.value}
                </p>
              </div>

              <div
                className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${card.bg}`}
              >
                <card.icon className={`size-4 ${card.color}`} />
              </div>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  )
}

async function ChartsSection({
  dashboardData,
  entries,
  dict,
}: {
  dashboardData: DashboardView | null
  entries: ReportEntry[]
  dict: Dictionary
}) {
  if (!dashboardData) return null

  const byPlatform: Record<string, number> = {}

  for (const entry of entries) {
    byPlatform[entry.platform] = (byPlatform[entry.platform] ?? 0) + entry.grossValue
  }

  const totalGross = Object.values(byPlatform).reduce((sum, value) => sum + value, 0)

  const realBreakdown: PlatformBreakdown[] = Object.entries(byPlatform)
    .sort(([, a], [, b]) => b - a)
    .map(([platform, amount]) => ({
      platform,
      amount,
      percentage: totalGross > 0 ? Math.round((amount / totalGross) * 100) : 0,
    }))

  const breakdown = realBreakdown.length > 0 ? realBreakdown : dashboardData.platformBreakdown

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
      <RevenueChart data={dashboardData} dict={dict} />
      <PlatformBreakdown breakdown={breakdown} dict={dict} />
    </div>
  )
}

function RevenueChart({
  data,
  dict,
}: {
  data: DashboardView
  dict: Dictionary
}) {
  const max = Math.max(...data.monthlyEvolution.map((item) => item.grossRevenue), 1)

  return (
    <Card className="rounded-2xl border-slate-200 bg-white shadow-sm shadow-slate-200/40 lg:col-span-2">
      <CardHeader className="border-b border-slate-100 px-5 py-4">
        <CardTitle className="text-sm font-semibold text-slate-900">
          {dict.dashboard.monthlyEvolution}
        </CardTitle>
      </CardHeader>

      <CardContent className="p-5">
        <div className="flex h-48 items-end gap-2">
          {data.monthlyEvolution.map((item) => (
            <div key={item.month} className="flex flex-1 flex-col items-center gap-2">
              <div className="flex h-40 w-full flex-col justify-end gap-0.5">
                <div
                  className="w-full rounded-t-md bg-blue-100"
                  style={{ height: `${(item.grossRevenue / max) * 100}%` }}
                  title={`${dict.dashboard.grossRevenueTooltip}: ${formatCurrency(
                    item.grossRevenue
                  )}`}
                />
                <div
                  className="w-full rounded-t-md bg-blue-600"
                  style={{ height: `${(item.received / max) * 100}%` }}
                  title={`${dict.dashboard.receivedTooltip}: ${formatCurrency(
                    item.received
                  )}`}
                />
              </div>

              <span className="text-xs text-slate-500">{item.month}</span>
            </div>
          ))}
        </div>

        <div className="mt-5 flex items-center gap-5 border-t border-slate-100 pt-4">
          <div className="flex items-center gap-2 text-xs text-slate-500">
            <div className="size-2.5 rounded-sm bg-blue-100" />
            {dict.dashboard.grossRevenueLegend}
          </div>

          <div className="flex items-center gap-2 text-xs text-slate-500">
            <div className="size-2.5 rounded-sm bg-blue-600" />
            {dict.dashboard.receivedLegend}
          </div>
        </div>
      </CardContent>
    </Card>
  )
}

function PlatformBreakdown({
  breakdown,
  dict,
}: {
  breakdown: PlatformBreakdown[]
  dict: Dictionary
}) {
  const colors = [
    'bg-blue-600',
    'bg-emerald-500',
    'bg-amber-500',
    'bg-violet-500',
    'bg-rose-500',
  ]

  if (breakdown.length === 0) {
    return (
      <Card className="rounded-2xl border-slate-200 bg-white shadow-sm shadow-slate-200/40">
        <CardHeader className="border-b border-slate-100 px-5 py-4">
          <CardTitle className="text-sm font-semibold text-slate-900">
            {dict.dashboard.marketplaces}
          </CardTitle>
        </CardHeader>

        <CardContent className="flex h-40 items-center justify-center text-sm text-slate-500">
          {dict.dashboard.noOrdersFound}
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="rounded-2xl border-slate-200 bg-white shadow-sm shadow-slate-200/40">
      <CardHeader className="border-b border-slate-100 px-5 py-4">
        <CardTitle className="text-sm font-semibold text-slate-900">
          {dict.dashboard.marketplaces}
        </CardTitle>
      </CardHeader>

      <CardContent className="space-y-5 p-5">
        {breakdown.map((platform, index) => (
          <div key={platform.platform} className="space-y-2">
            <div className="flex items-center justify-between text-sm">
              <span className="font-medium text-slate-900">
                {platform.platform}
              </span>
              <span className="text-slate-500">{platform.percentage}%</span>
            </div>

            <div className="h-2 overflow-hidden rounded-full bg-slate-100">
              <div
                className={`h-full rounded-full ${
                  colors[index] ?? 'bg-slate-400'
                } transition-all`}
                style={{ width: `${platform.percentage}%` }}
              />
            </div>

            <p className="text-xs text-slate-500">
              {formatCurrency(platform.amount)}
            </p>
          </div>
        ))}
      </CardContent>
    </Card>
  )
}

function RecentOrdersTable({
  orders,
  dict,
  lang,
}: {
  orders: ReportEntry[]
  dict: Dictionary
  lang: Locale
}) {
  return (
    <Card className="overflow-hidden rounded-2xl border-slate-200 bg-white shadow-sm shadow-slate-200/40">
      <CardHeader className="flex-row items-center justify-between border-b border-slate-100 px-5 py-4">
        <CardTitle className="text-sm font-semibold text-slate-900">
          {dict.dashboard.recentOrders}
        </CardTitle>

        <a
          href={`/${lang}/lancamentos`}
          className="rounded-lg px-2 py-1 text-xs font-medium text-blue-600 transition hover:bg-blue-50"
        >
          {dict.common.viewAll} →
        </a>
      </CardHeader>

      <CardContent className="p-0">
        <div className="scroll-hidden overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-slate-50/80">
              <tr className="border-b border-slate-100">
                <th className="px-5 py-3 text-left text-xs font-medium text-slate-500">
                  {dict.dashboard.table.order}
                </th>
                <th className="px-5 py-3 text-left text-xs font-medium text-slate-500">
                  {dict.dashboard.table.buyer}
                </th>
                <th className="hidden px-5 py-3 text-left text-xs font-medium text-slate-500 md:table-cell">
                  {dict.dashboard.table.platform}
                </th>
                <th className="hidden px-5 py-3 text-left text-xs font-medium text-slate-500 lg:table-cell">
                  {dict.dashboard.table.date}
                </th>
                <th className="px-5 py-3 text-right text-xs font-medium text-slate-500">
                  {dict.dashboard.table.value}
                </th>
                <th className="px-5 py-3 text-left text-xs font-medium text-slate-500">
                  {dict.dashboard.table.status}
                </th>
              </tr>
            </thead>

            <tbody>
              {orders.map((order) => (
                <tr
                  key={order.id}
                  className="border-b border-slate-100 transition-colors last:border-0 hover:bg-slate-50/70"
                >
                  <td className="px-5 py-4 font-mono text-xs text-slate-500">
                    {order.orderId}
                  </td>
                  <td className="px-5 py-4 font-medium text-slate-900">
                    {order.buyerName}
                  </td>
                  <td className="hidden px-5 py-4 md:table-cell">
                    <PlatformBadge platform={order.platform} />
                  </td>
                  <td className="hidden px-5 py-4 text-slate-500 lg:table-cell">
                    {formatDate(order.saleDate)}
                  </td>
                  <td className="px-5 py-4 text-right font-semibold text-slate-900">
                    {formatCurrency(order.grossValue)}
                  </td>
                  <td className="px-5 py-4">
                    <OrderStatusBadge status={order.status} dict={dict} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </CardContent>
    </Card>
  )
}

function PlatformBadge({ platform }: { platform: string }) {
  const map: Record<string, string> = {
    'Mercado Livre': 'bg-yellow-50 text-yellow-700 ring-yellow-200',
    Shopee: 'bg-orange-50 text-orange-700 ring-orange-200',
    Amazon: 'bg-blue-50 text-blue-700 ring-blue-200',
  }

  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium ring-1 ${
        map[platform] ?? 'bg-slate-50 text-slate-600 ring-slate-200'
      }`}
    >
      {platform}
    </span>
  )
}

function OrderStatusBadge({
  status,
  dict,
}: {
  status: ReportEntry['status']
  dict: Dictionary
}) {
  const variants: Record<string, 'success' | 'warning' | 'destructive' | 'secondary'> = {
    PAID: 'success',
    PENDING: 'warning',
    CANCELLED: 'destructive',
    REFUNDED: 'secondary',
  }

  const label = dict.dashboard.status[status] ?? status
  const variant = variants[status] ?? 'secondary'

  return <Badge variant={variant}>{label}</Badge>
}

function KpiSkeleton() {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {Array.from({ length: 4 }).map((_, index) => (
        <Card
          key={index}
          className="rounded-2xl border-slate-200 bg-white shadow-sm shadow-slate-200/40"
        >
          <CardContent className="p-5">
            <Skeleton className="h-4 w-24" />
            <Skeleton className="mt-3 h-8 w-32" />
          </CardContent>
        </Card>
      ))}
    </div>
  )
}

function ChartSkeleton() {
  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
      <Card className="rounded-2xl border-slate-200 bg-white shadow-sm shadow-slate-200/40 lg:col-span-2">
        <CardHeader className="border-b border-slate-100 px-5 py-4">
          <Skeleton className="h-4 w-32" />
        </CardHeader>
        <CardContent className="p-5">
          <Skeleton className="h-48 w-full" />
        </CardContent>
      </Card>

      <Card className="rounded-2xl border-slate-200 bg-white shadow-sm shadow-slate-200/40">
        <CardHeader className="border-b border-slate-100 px-5 py-4">
          <Skeleton className="h-4 w-28" />
        </CardHeader>
        <CardContent className="space-y-4 p-5">
          {Array.from({ length: 3 }).map((_, index) => (
            <Skeleton key={index} className="h-10 w-full" />
          ))}
        </CardContent>
      </Card>
    </div>
  )
}

function OrdersSkeleton() {
  return (
    <Card className="rounded-2xl border-slate-200 bg-white shadow-sm shadow-slate-200/40">
      <CardHeader className="border-b border-slate-100 px-5 py-4">
        <Skeleton className="h-4 w-32" />
      </CardHeader>
      <CardContent className="space-y-3 p-5">
        {Array.from({ length: 5 }).map((_, index) => (
          <Skeleton key={index} className="h-10 w-full" />
        ))}
      </CardContent>
    </Card>
  )
}