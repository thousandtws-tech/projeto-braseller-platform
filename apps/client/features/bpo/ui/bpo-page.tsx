import type { Metadata } from 'next'
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
import type { AccountantClient, AccountingClosing, ReportsDre, ReportsSummary } from '@/shared/types'
import { BatchClosingAction } from './batch-closing-action'

export const metadata: Metadata = { title: 'BPO' }

const CURRENT_PERIOD = (() => {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth()
  const from = `${year}-${String(month + 1).padStart(2, '0')}-01`
  const lastDay = new Date(year, month + 1, 0).getDate()
  const to = `${year}-${String(month + 1).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
  const label = new Date(year, month, 1).toLocaleDateString('pt-BR', {
    month: 'long',
    year: 'numeric',
  })

  const monthKey = `${year}-${String(month + 1).padStart(2, '0')}`

  return { from, to, label, monthKey }
})()

interface ClientRow {
  client: AccountantClient
  summary: ReportsSummary | null
  dre: ReportsDre | null
  closing: AccountingClosing | null
}

export default async function BpoPage() {
  const token = (await getToken()) ?? ''
  const session = await getSession()
  const roles = session?.roles ?? []

  if (!isBpoOperator(roles)) {
    return (
      <div className="flex max-w-7xl flex-col gap-6">
        <div className="flex flex-col gap-1">
          <h2 className="text-xl font-semibold">BPO</h2>
          <p className="text-sm text-muted-foreground">Acesso restrito ao time BPO.</p>
        </div>
      </div>
    )
  }

  const clients = await getAccountantClients(token)
  const rows = await Promise.all(
    clients.map(async (client): Promise<ClientRow> => {
      const [summary, dre, closing] = await Promise.all([
        getReportsSummary(token, client.tenantId, {
          from: CURRENT_PERIOD.from,
          to: CURRENT_PERIOD.to,
        }),
        getReportsDre(token, client.tenantId, CURRENT_PERIOD.from, CURRENT_PERIOD.to).catch(() => null),
        getAccountingClosing(token, client.tenantId, CURRENT_PERIOD.monthKey).catch(() => null),
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
          <h2 className="text-xl font-semibold">BPO</h2>
          <p className="text-sm text-muted-foreground capitalize">{CURRENT_PERIOD.label}</p>
        </div>
        <div className="flex items-center gap-2">
          {isGlobalBpoOperator(roles) && <Badge variant="default">Carteira global</Badge>}
          <Badge variant="secondary">{totals.clients} clientes</Badge>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <KpiCard title="Clientes ativos" value={String(totals.clients)} icon={BriefcaseBusiness} />
        <KpiCard title="Receita" value={formatCurrency(totals.gross)} icon={DollarSign} />
        <KpiCard title="Taxas/Frete" value={formatCurrency(totals.fees)} icon={ArrowDownLeft} />
        <KpiCard title="Lucro disponível" value={formatCurrency(totals.distributable)} icon={Wallet} />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[1fr_280px]">
        <BatchClosingAction
          periodMonth={CURRENT_PERIOD.monthKey}
          tenantIds={pendingRows.map((row) => row.client.tenantId)}
        />
        <Card>
          <CardContent className="flex h-full items-center justify-between gap-4 pt-6">
            <div>
              <p className="text-xs text-muted-foreground">Operacao do periodo</p>
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
          <CardTitle>Clientes atendidos</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {rows.length === 0 ? (
            <div className="flex flex-col items-center justify-center gap-2 px-6 py-12 text-center">
              <div className="flex size-12 items-center justify-center rounded-full bg-muted">
                <BriefcaseBusiness className="size-6 text-muted-foreground" />
              </div>
              <p className="text-sm font-medium">Nenhum cliente vinculado</p>
              <p className="max-w-sm text-xs text-muted-foreground">
                Os clientes aparecem aqui quando um tenant concede acesso ao contador ou quando o usuario tem acesso global BPO.
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="pl-6">Cliente</TableHead>
                    <TableHead>Receita</TableHead>
                    <TableHead>Taxas/Frete</TableHead>
                    <TableHead>Pedidos</TableHead>
                    <TableHead>Lucro disponível</TableHead>
                    <TableHead className="pr-6 text-right">Status</TableHead>
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
                        <ClientStatus row={row} />
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

function ClientStatus({ row }: { row: ClientRow }) {
  if (!hasActiveBpoAccess(row.client) || row.client.tenantStatus !== 'ACTIVE') {
    return <Badge variant="secondary">Inativo</Badge>
  }

  if (row.closing) {
    return (
      <Badge variant="success">
        <FileSignature className="size-3" />
        Fechado
      </Badge>
    )
  }

  if (!row.dre) {
    return <Badge variant="warning">Sem DRE</Badge>
  }

  if ((row.summary?.entry_count ?? 0) === 0) {
    return <Badge variant="secondary">Sem vendas</Badge>
  }

  return (
    <Badge variant="warning">
      <Clock3 className="size-3" />
      Pendente
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
