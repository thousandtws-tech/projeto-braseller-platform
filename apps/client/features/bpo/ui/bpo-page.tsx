import { BriefcaseBusiness, DollarSign, ArrowDownLeft, Wallet, FileSignature, Clock3, type LucideIcon } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Badge } from '@/shared/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/shared/ui/table'
import { getToken, getSession } from '@/entities/session/server/session'
import { formatCurrency, getAccountantClients, getAccountingClosing, getReportsDre, getReportsSummary } from '@/shared/api/gateway'
import { isBpoOperator, isGlobalBpoOperator } from '@/entities/session/model/permissions'
import { getDictionary, type Dictionary } from '@/shared/i18n/get-dictionary'
import { formatMessage } from '@/shared/i18n/format'
import type { Locale } from '@/shared/i18n/config'
import type { AccountantClient, AccountingClosing, ReportsDre, ReportsSummary } from '@/shared/types'
import { BatchClosingAction } from './batch-closing-action'

const LOCALE_MAP: Record<Locale, string> = { 'pt-BR': 'pt-BR', en: 'en-US', es: 'es-ES' }

interface Props {
  params: Promise<{ lang: Locale }>
}

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.bpo.title }
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

  const monthKey = `${year}-${String(month + 1).padStart(2, '0')}`

  return { from, to, label, monthKey }
}

interface ClientRow {
  client: AccountantClient
  summary: ReportsSummary | null
  dre: ReportsDre | null
  closing: AccountingClosing | null
}

export default async function BpoPage({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  const token = (await getToken()) ?? ''
  const session = await getSession()
  const roles = session?.roles ?? []
  const currentPeriod = getCurrentPeriod(lang)

  if (!isBpoOperator(roles)) {
    return (
      <div className="flex max-w-7xl flex-col gap-6">
        <div className="flex flex-col gap-1">
          <h2 className="text-xl font-semibold">{dict.bpo.title}</h2>
          <p className="text-sm text-muted-foreground">{dict.bpo.restricted}</p>
        </div>
      </div>
    )
  }

  const clients = await getAccountantClients(token)
  const rows = await Promise.all(
    clients.map(async (client): Promise<ClientRow> => {
      const [summary, dre, closing] = await Promise.all([
        getReportsSummary(token, client.tenantId, {
          from: currentPeriod.from,
          to: currentPeriod.to,
        }),
        getReportsDre(token, client.tenantId, currentPeriod.from, currentPeriod.to).catch(() => null),
        getAccountingClosing(token, client.tenantId, currentPeriod.monthKey).catch(() => null),
      ])

      return { client, summary, dre, closing }
    })
  )

  const totals = rows.reduce(
    (acc, row) => ({
      clients: acc.clients + 1,
      gross: acc.gross + (row.summary?.gross_value ?? 0),
      fees: acc.fees + (row.summary?.fee_value ?? 0),
      distributable: acc.distributable + (row.dre?.distributable_profit ?? 0),
      orders: acc.orders + (row.summary?.entry_count ?? 0),
    }),
    { clients: 0, gross: 0, fees: 0, distributable: 0, orders: 0 }
  )
  const pendingRows = rows.filter(isPendingForBatchClosing)
  const signedRows = rows.filter((row) => row.closing).length

  return (
    <div className="flex max-w-7xl flex-col gap-6">
      <div className="flex items-center justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h2 className="text-xl font-semibold">{dict.bpo.title}</h2>
          <p className="text-sm text-muted-foreground capitalize">{currentPeriod.label}</p>
        </div>
        <div className="flex items-center gap-2">
          {isGlobalBpoOperator(roles) && <Badge variant="default">{dict.bpo.globalPortfolio}</Badge>}
          <Badge variant="secondary">{formatMessage(dict.bpo.clientsBadge, { count: totals.clients })}</Badge>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <KpiCard title={dict.bpo.kpis.activeClients} value={String(totals.clients)} icon={BriefcaseBusiness} />
        <KpiCard title={dict.bpo.kpis.revenue} value={formatCurrency(totals.gross)} icon={DollarSign} />
        <KpiCard title={dict.bpo.kpis.feesShipping} value={formatCurrency(totals.fees)} icon={ArrowDownLeft} />
        <KpiCard title={dict.bpo.kpis.availableProfit} value={formatCurrency(totals.distributable)} icon={Wallet} />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[1fr_280px]">
        <BatchClosingAction
          periodMonth={currentPeriod.monthKey}
          tenantIds={pendingRows.map((row) => row.client.tenantId)}
          dict={dict}
        />
        <Card>
          <CardContent className="flex h-full items-center justify-between gap-4 pt-6">
            <div>
              <p className="text-xs text-muted-foreground">{dict.bpo.periodOperation}</p>
              <p className="text-2xl font-bold">{signedRows}/{totals.clients}</p>
            </div>
            <div className="flex size-10 items-center justify-center rounded-lg bg-emerald-500/10">
              <FileSignature className="size-5 text-emerald-600" />
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>{dict.bpo.clientsTable.title}</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {rows.length === 0 ? (
            <div className="flex flex-col items-center justify-center gap-2 px-6 py-12 text-center">
              <div className="flex size-12 items-center justify-center rounded-full bg-muted">
                <BriefcaseBusiness className="size-6 text-muted-foreground" />
              </div>
              <p className="text-sm font-medium">{dict.bpo.clientsTable.empty.title}</p>
              <p className="max-w-sm text-xs text-muted-foreground">
                {dict.bpo.clientsTable.empty.hint}
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="pl-6">{dict.bpo.clientsTable.columns.client}</TableHead>
                    <TableHead>{dict.bpo.clientsTable.columns.revenue}</TableHead>
                    <TableHead>{dict.bpo.clientsTable.columns.feesShipping}</TableHead>
                    <TableHead>{dict.bpo.clientsTable.columns.orders}</TableHead>
                    <TableHead>{dict.bpo.clientsTable.columns.availableProfit}</TableHead>
                    <TableHead className="pr-6 text-right">{dict.bpo.clientsTable.columns.status}</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {rows.map((row) => (
                    <TableRow key={row.client.tenantId}>
                      <TableCell className="pl-6">
                        <div className="flex min-w-52 flex-col gap-0.5">
                          <span className="font-medium">
                            {row.client.tradeName || row.client.legalName}
                          </span>
                          <span className="text-xs text-muted-foreground">{row.client.legalName}</span>
                        </div>
                      </TableCell>
                      <TableCell>{formatCurrency(row.summary?.gross_value ?? 0)}</TableCell>
                      <TableCell>{formatCurrency(row.summary?.fee_value ?? 0)}</TableCell>
                      <TableCell>{row.summary?.entry_count ?? 0}</TableCell>
                      <TableCell>{formatCurrency(row.dre?.distributable_profit ?? 0)}</TableCell>
                      <TableCell className="pr-6 text-right">
                        <ClientStatus row={row} dict={dict} />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

function KpiCard({ title, value, icon: Icon }: { title: string; value: string; icon: LucideIcon }) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex items-start justify-between gap-3">
          <p className="text-2xl font-bold text-foreground">{value}</p>
          <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10">
            <Icon className="size-4 text-primary" />
          </div>
        </div>
      </CardContent>
    </Card>
  )
}

function ClientStatus({ row, dict }: { row: ClientRow; dict: Dictionary }) {
  if (!hasActiveBpoAccess(row.client) || row.client.tenantStatus !== 'ACTIVE') {
    return <Badge variant="secondary">{dict.bpo.status.inactive}</Badge>
  }

  if (row.closing) {
    return (
      <Badge variant="success">
        <FileSignature className="size-3" />
        {dict.bpo.status.closed}
      </Badge>
    )
  }

  if (!row.dre) {
    return <Badge variant="warning">{dict.bpo.status.noDre}</Badge>
  }

  if ((row.summary?.entry_count ?? 0) === 0) {
    return <Badge variant="secondary">{dict.bpo.status.noSales}</Badge>
  }

  return (
    <Badge variant="warning">
      <Clock3 className="size-3" />
      {dict.bpo.status.pending}
    </Badge>
  )
}

function isPendingForBatchClosing(row: ClientRow): boolean {
  return hasActiveBpoAccess(row.client)
    && row.client.tenantStatus === 'ACTIVE'
    && !row.closing
    && row.dre !== null
    && (row.summary?.entry_count ?? 0) > 0
}

function hasActiveBpoAccess(client: AccountantClient): boolean {
  return client.accessStatus === 'ACTIVE' || client.accessStatus === 'GLOBAL'
}
