import { Suspense } from 'react'
import { TrendingUp, ShoppingCart, DollarSign, ArrowDownLeft } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Badge } from '@/shared/ui/badge'
import { Skeleton } from '@/shared/ui/skeleton'
import { DatePicker } from '@/shared/ui/date-picker'
import { getToken, getSession } from '@/entities/session/server/session'
import { getDashboard, getReportEntries, getReportsSummary, formatCurrency, formatDate } from '@/shared/api/gateway'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'
import type { DashboardView, PlatformBreakdown, ReportEntry, ReportsSummary } from '@/shared/types'

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
  return { title: dict.dashboard.title }
}

export default async function DashboardPage({ params }: PageProps) {
  const { lang } = await params
  const token = (await getToken()) ?? ''
  const session = await getSession()
  const tenantId = session?.tenantId
  const dict = await getDictionary(lang)
  const period = getCurrentPeriod(lang)

  return (
    <div className="space-y-6 max-w-7xl">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold">{dict.dashboard.title}</h2>
          <p className="text-sm text-muted-foreground capitalize">{period.label}</p>
        </div>
        <PeriodSelector dict={dict} period={period} />
      </div>

      <Suspense fallback={<KpiSkeleton />}>
        <KpiSection token={token} tenantId={tenantId} period={period} dict={dict} />
      </Suspense>

      <Suspense fallback={<ChartSkeleton />}>
        <ChartsSection token={token} tenantId={tenantId} dict={dict} />
      </Suspense>

      <Suspense fallback={<OrdersSkeleton />}>
        <RecentOrdersSection token={token} tenantId={tenantId} dict={dict} lang={lang} />
      </Suspense>
    </div>
  )
}

function PeriodSelector({ dict, period }: { dict: Dictionary; period: { from: string } }) {
  return (
    <DatePicker
      name="period"
      defaultValue={period.from}
      displayFormat="LLLL yyyy"
      placeholder={dict.dashboard.selectPeriod}
      buttonClassName="h-8 w-40 capitalize"
    />
  )
}

async function KpiSection({
  token,
  tenantId,
  period,
  dict,
}: {
  token: string
  tenantId?: string
  period: { from: string; to: string }
  dict: Dictionary
}) {
  const summary = tenantId
    ? await getReportsSummary(token, tenantId, {
        from: period.from,
        to: period.to,
      })
    : null

  return <KpiCards summary={summary} dict={dict} />
}

function KpiCards({ summary, dict }: { summary: ReportsSummary | null; dict: Dictionary }) {
  const cards = [
    {
      title: dict.dashboard.kpis.grossRevenue,
      value: formatCurrency(summary?.gross_value ?? 0),
      icon: DollarSign,
      color: 'text-primary',
      bg: 'bg-primary/10',
    },
    {
      title: dict.dashboard.kpis.received,
      value: formatCurrency(summary?.received_value ?? 0),
      icon: TrendingUp,
      color: 'text-[--success]',
      bg: 'bg-[--success]/10',
    },
    {
      title: dict.dashboard.kpis.feesAndShipping,
      value: formatCurrency(summary?.fee_value ?? 0),
      icon: ArrowDownLeft,
      color: 'text-destructive',
      bg: 'bg-destructive/10',
    },
    {
      title: dict.dashboard.kpis.totalOrders,
      value: String(summary?.entry_count ?? 0),
      icon: ShoppingCart,
      color: 'text-[--warning]',
      bg: 'bg-[--warning]/10',
    },
  ]

  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((card) => (
        <Card key={card.title}>
          <CardHeader className="pb-2">
            <CardTitle>{card.title}</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-start justify-between">
              <div>
                <p className="text-2xl font-bold text-foreground">{card.value}</p>
              </div>
              <div className={`size-9 rounded-lg ${card.bg} flex items-center justify-center`}>
                <card.icon className={`size-4 ${card.color}`} />
              </div>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  )
}

async function ChartsSection({ token, tenantId, dict }: { token: string; tenantId?: string; dict: Dictionary }) {
  const [data, entries] = await Promise.all([
    getDashboard(token, tenantId),
    getReportEntries(token, tenantId),
  ])

  // Calcula breakdown real por plataforma a partir dos pedidos do core-service
  const byPlatform: Record<string, number> = {}
  for (const e of entries) {
    byPlatform[e.platform] = (byPlatform[e.platform] ?? 0) + e.grossValue
  }

  const totalGross = Object.values(byPlatform).reduce((s, v) => s + v, 0)

  const realBreakdown: PlatformBreakdown[] = Object.entries(byPlatform)
    .sort(([, a], [, b]) => b - a)
    .map(([platform, amount]) => ({
      platform,
      amount,
      percentage: totalGross > 0 ? Math.round((amount / totalGross) * 100) : 0,
    }))

  // Use dashboard platform totals only when no entry rows are available.
  const breakdown = realBreakdown.length > 0 ? realBreakdown : data.platformBreakdown

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <RevenueChart data={data} dict={dict} />
      <PlatformBreakdown breakdown={breakdown} dict={dict} />
    </div>
  )
}

function RevenueChart({ data, dict }: { data: DashboardView; dict: Dictionary }) {
  const max = Math.max(...data.monthlyEvolution.map((m) => m.grossRevenue))
  return (
    <Card className="lg:col-span-2">
      <CardHeader>
        <CardTitle>{dict.dashboard.monthlyEvolution}</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex items-end gap-2 h-44">
          {data.monthlyEvolution.map((m) => (
            <div key={m.month} className="flex-1 flex flex-col items-center gap-1">
              <div className="w-full flex flex-col gap-0.5 justify-end" style={{ height: '160px' }}>
                <div
                  className="w-full rounded-t-sm bg-primary/20"
                  style={{ height: `${(m.grossRevenue / max) * 100}%` }}
                  title={`${dict.dashboard.grossRevenueTooltip}: ${formatCurrency(m.grossRevenue)}`}
                />
                <div
                  className="w-full rounded-t-sm bg-primary"
                  style={{ height: `${(m.received / max) * 100}%` }}
                  title={`${dict.dashboard.receivedTooltip}: ${formatCurrency(m.received)}`}
                />
              </div>
              <span className="text-xs text-muted-foreground">{m.month}</span>
            </div>
          ))}
        </div>
        <div className="flex items-center gap-4 mt-4 pt-4 border-t border-border">
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <div className="size-2.5 rounded-sm bg-primary/20" />{dict.dashboard.grossRevenueLegend}
          </div>
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <div className="size-2.5 rounded-sm bg-primary" />{dict.dashboard.receivedLegend}
          </div>
        </div>
      </CardContent>
    </Card>
  )
}

function PlatformBreakdown({ breakdown, dict }: { breakdown: PlatformBreakdown[]; dict: Dictionary }) {
  const colors = ['bg-primary', 'bg-chart-2', 'bg-chart-3', 'bg-chart-4', 'bg-chart-5']

  if (breakdown.length === 0) {
    return (
      <Card>
        <CardHeader><CardTitle>{dict.dashboard.marketplaces}</CardTitle></CardHeader>
        <CardContent className="flex items-center justify-center h-32 text-sm text-muted-foreground">
          {dict.dashboard.noOrdersFound}
        </CardContent>
      </Card>
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{dict.dashboard.marketplaces}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {breakdown.map((p, i) => (
          <div key={p.platform} className="space-y-1.5">
            <div className="flex items-center justify-between text-sm">
              <span className="font-medium">{p.platform}</span>
              <span className="text-muted-foreground">{p.percentage}%</span>
            </div>
            <div className="h-2 bg-muted rounded-full overflow-hidden">
              <div
                className={`h-full rounded-full ${colors[i] ?? 'bg-muted-foreground'} transition-all`}
                style={{ width: `${p.percentage}%` }}
              />
            </div>
            <p className="text-xs text-muted-foreground">{formatCurrency(p.amount)}</p>
          </div>
        ))}
      </CardContent>
    </Card>
  )
}

async function RecentOrdersSection({
  token,
  tenantId,
  dict,
  lang,
}: {
  token: string
  tenantId?: string
  dict: Dictionary
  lang: Locale
}) {
  const data = await getDashboard(token, tenantId)
  return <RecentOrdersTable orders={data.recentOrders} dict={dict} lang={lang} />
}

function RecentOrdersTable({ orders, dict, lang }: { orders: ReportEntry[]; dict: Dictionary; lang: Locale }) {
  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between">
        <CardTitle>{dict.dashboard.recentOrders}</CardTitle>
        <a href={`/${lang}/lancamentos`} className="text-xs text-primary hover:underline">
          {dict.common.viewAll} →
        </a>
      </CardHeader>
      <CardContent className="p-0">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border">
                <th className="text-left px-5 py-3 text-xs font-medium text-muted-foreground">{dict.dashboard.table.order}</th>
                <th className="text-left px-5 py-3 text-xs font-medium text-muted-foreground">{dict.dashboard.table.buyer}</th>
                <th className="text-left px-5 py-3 text-xs font-medium text-muted-foreground hidden md:table-cell">{dict.dashboard.table.platform}</th>
                <th className="text-left px-5 py-3 text-xs font-medium text-muted-foreground hidden lg:table-cell">{dict.dashboard.table.date}</th>
                <th className="text-right px-5 py-3 text-xs font-medium text-muted-foreground">{dict.dashboard.table.value}</th>
                <th className="text-left px-5 py-3 text-xs font-medium text-muted-foreground">{dict.dashboard.table.status}</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((order) => (
                <tr key={order.id} className="border-b border-border/50 hover:bg-muted/30 transition-colors">
                  <td className="px-5 py-3 font-mono text-xs text-muted-foreground">{order.orderId}</td>
                  <td className="px-5 py-3 font-medium">{order.buyerName}</td>
                  <td className="px-5 py-3 hidden md:table-cell">
                    <PlatformBadge platform={order.platform} />
                  </td>
                  <td className="px-5 py-3 text-muted-foreground hidden lg:table-cell">
                    {formatDate(order.saleDate)}
                  </td>
                  <td className="px-5 py-3 text-right font-medium">{formatCurrency(order.grossValue)}</td>
                  <td className="px-5 py-3"><OrderStatusBadge status={order.status} dict={dict} /></td>
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
    'Mercado Livre': 'bg-yellow-100 text-yellow-800',
    'Shopee': 'bg-orange-100 text-orange-800',
    'Amazon': 'bg-blue-100 text-blue-800',
  }
  return (
    <span className={`inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium ${map[platform] ?? 'bg-muted text-muted-foreground'}`}>
      {platform}
    </span>
  )
}

function OrderStatusBadge({ status, dict }: { status: ReportEntry['status']; dict: Dictionary }) {
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
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      {Array.from({ length: 4 }).map((_, i) => (
        <Card key={i}>
          <CardHeader><Skeleton className="h-4 w-24" /></CardHeader>
          <CardContent><Skeleton className="h-8 w-32" /></CardContent>
        </Card>
      ))}
    </div>
  )
}

function ChartSkeleton() {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <Card className="lg:col-span-2">
        <CardHeader><Skeleton className="h-4 w-32" /></CardHeader>
        <CardContent><Skeleton className="h-48 w-full" /></CardContent>
      </Card>
      <Card>
        <CardHeader><Skeleton className="h-4 w-28" /></CardHeader>
        <CardContent className="space-y-4">
          {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
        </CardContent>
      </Card>
    </div>
  )
}

function OrdersSkeleton() {
  return (
    <Card>
      <CardHeader><Skeleton className="h-4 w-32" /></CardHeader>
      <CardContent className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
      </CardContent>
    </Card>
  )
}
