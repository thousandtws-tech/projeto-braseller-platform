import Link from 'next/link'
import { Activity, AlertCircle, CheckCircle2, Clock, XCircle } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Badge } from '@/shared/ui/badge'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/shared/ui/table'
import { cn } from '@/shared/lib/utils'
import { getToken } from '@/entities/session/server/session'
import { getIntegrationsHealth, getIntegrationLogs } from '@/shared/api/gateway'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import { formatMessage } from '@/shared/i18n/format'
import type { Locale } from '@/shared/i18n/config'
import type { IntegrationHealthSummary, IntegrationEventLog } from '@/shared/types'

const LOCALE_MAP: Record<Locale, string> = { 'pt-BR': 'pt-BR', en: 'en-US', es: 'es-ES' }

const INTEGRATION_NAMES = ['mercado_livre', 'shopee', 'amazon', 'brasil_api', 'clicksign', 'open_finance'] as const

const SEVERITIES = ['INFO', 'WARNING', 'CRITICAL'] as const

const STATUS_META: Record<IntegrationHealthSummary['current_status'], { badge: 'success' | 'warning' | 'destructive'; icon: typeof CheckCircle2 }> = {
  UP: { badge: 'success', icon: CheckCircle2 },
  DEGRADED: { badge: 'warning', icon: AlertCircle },
  DOWN: { badge: 'destructive', icon: XCircle },
}

const SEVERITY_BADGE: Record<IntegrationEventLog['severity'], 'secondary' | 'warning' | 'destructive'> = {
  INFO: 'secondary',
  WARNING: 'warning',
  CRITICAL: 'destructive',
}

interface Props {
  params: Promise<{ lang: Locale }>
  searchParams: Promise<{ integration?: string; severity?: string }>
}

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.integrationsHealth.title }
}

function unknownSummary(integrationName: string): IntegrationHealthSummary {
  return {
    integration_name: integrationName,
    current_status: 'UP',
    requests_24h: 0,
    failures_24h: 0,
  }
}

function formatDateTime(value: string | undefined, lang: Locale) {
  if (!value) return null
  return new Date(value).toLocaleString(LOCALE_MAP[lang], {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

export default async function IntegrationsHealthPage({ params, searchParams }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  const { integration: integrationParam, severity: severityParam } = await searchParams
  const token = (await getToken()) ?? ''

  const health = await getIntegrationsHealth(token)
  const healthByName = new Map(health.map((h) => [h.integration_name, h]))

  const selectedIntegration = INTEGRATION_NAMES.includes(integrationParam as typeof INTEGRATION_NAMES[number])
    ? (integrationParam as string)
    : INTEGRATION_NAMES[0]

  const selectedSeverity = SEVERITIES.includes(severityParam as typeof SEVERITIES[number]) ? severityParam : undefined

  const logs = await getIntegrationLogs(token, selectedIntegration, selectedSeverity)

  const integrationNames = dict.integrationsHealth.integrations as Record<string, string>
  const statusLabels = dict.integrationsHealth.status as Record<string, string>
  const severityLabels = dict.integrationsHealth.severity as Record<string, string>
  const failureTypeLabels = dict.integrationsHealth.failureType as Record<string, string>
  const outcomeLabels = dict.integrationsHealth.outcome as Record<string, string>

  return (
    <div className="space-y-6 max-w-6xl">
      <div>
        <h2 className="text-xl font-semibold">{dict.integrationsHealth.title}</h2>
        <p className="text-sm text-muted-foreground">{dict.integrationsHealth.subtitle}</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {INTEGRATION_NAMES.map((name) => {
          const summary = healthByName.get(name) ?? unknownSummary(name)
          const meta = STATUS_META[summary.current_status]
          const StatusIcon = meta.icon
          const active = name === selectedIntegration
          const lastCheck = formatDateTime(summary.last_check_at, lang)
          const lastFailure = formatDateTime(summary.last_failure_at, lang)

          return (
            <Link key={name} href={`?integration=${name}`} scroll={false}>
              <Card className={cn('transition-colors hover:border-primary/40', active && 'border-primary')}>
                <CardHeader className="flex-row items-center justify-between gap-3 pb-2">
                  <div className="flex items-center gap-2.5">
                    <div className="size-8 rounded-lg bg-primary/10 flex items-center justify-center">
                      <Activity className="size-4 text-primary" />
                    </div>
                    <CardTitle className="text-sm">{integrationNames[name] ?? name}</CardTitle>
                  </div>
                  <Badge variant={meta.badge} className="gap-1">
                    <StatusIcon className="size-3" />
                    {statusLabels[summary.current_status] ?? summary.current_status}
                  </Badge>
                </CardHeader>
                <CardContent className="space-y-2 text-sm">
                  <div className="flex items-center justify-between">
                    <span className="text-muted-foreground">{dict.integrationsHealth.summary.availability}</span>
                    <span className="font-medium">
                      {summary.availability_pct_24h != null ? `${summary.availability_pct_24h}%` : '—'}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-muted-foreground">{dict.integrationsHealth.summary.avgResponseTime}</span>
                    <span className="font-medium">
                      {summary.avg_response_time_ms != null
                        ? formatMessage(dict.integrationsHealth.summary.msUnit, { value: summary.avg_response_time_ms })
                        : '—'}
                    </span>
                  </div>
                  <div className="flex items-center justify-between text-muted-foreground">
                    <span className="flex items-center gap-1.5">
                      <Clock className="size-3" />
                      {dict.integrationsHealth.summary.lastCheck}
                    </span>
                    <span>{lastCheck ?? dict.integrationsHealth.summary.never}</span>
                  </div>
                  {summary.failures_24h > 0 && (
                    <div className="flex items-center justify-between text-muted-foreground">
                      <span>{dict.integrationsHealth.summary.lastFailure}</span>
                      <span>{lastFailure ?? dict.integrationsHealth.summary.never}</span>
                    </div>
                  )}
                  <div className="flex items-center justify-between text-xs text-muted-foreground pt-1">
                    <span>{formatMessage(dict.integrationsHealth.summary.requests24h, { count: summary.requests_24h })}</span>
                    <span>{formatMessage(dict.integrationsHealth.summary.failures24h, { count: summary.failures_24h })}</span>
                  </div>
                </CardContent>
              </Card>
            </Link>
          )
        })}
      </div>

      <Card>
        <CardHeader className="flex-row items-center justify-between gap-3 flex-wrap pb-3">
          <div>
            <CardTitle>{dict.integrationsHealth.table.title}</CardTitle>
            <p className="text-sm text-muted-foreground">
              {formatMessage(dict.integrationsHealth.table.subtitle, { integration: integrationNames[selectedIntegration] ?? selectedIntegration })}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Link href={`?integration=${selectedIntegration}`} scroll={false}>
              <Badge variant={!selectedSeverity ? 'default' : 'outline'} className="cursor-pointer">
                {dict.integrationsHealth.filters.allSeverities}
              </Badge>
            </Link>
            {SEVERITIES.map((severity) => (
              <Link key={severity} href={`?integration=${selectedIntegration}&severity=${severity}`} scroll={false}>
                <Badge variant={selectedSeverity === severity ? SEVERITY_BADGE[severity] : 'outline'} className="cursor-pointer">
                  {severityLabels[severity] ?? severity}
                </Badge>
              </Link>
            ))}
          </div>
        </CardHeader>
        <CardContent className="p-0">
          {logs.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="pl-6">{dict.integrationsHealth.table.columns.time}</TableHead>
                  <TableHead>{dict.integrationsHealth.table.columns.endpoint}</TableHead>
                  <TableHead>{dict.integrationsHealth.table.columns.outcome}</TableHead>
                  <TableHead>{dict.integrationsHealth.table.columns.severity}</TableHead>
                  <TableHead>{dict.integrationsHealth.table.columns.responseTime}</TableHead>
                  <TableHead>{dict.integrationsHealth.table.columns.impact}</TableHead>
                  <TableHead className="pr-6">{dict.integrationsHealth.table.columns.action}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {logs.map((log) => (
                  <TableRow key={log.id}>
                    <TableCell className="pl-6 whitespace-nowrap text-xs text-muted-foreground">
                      {formatDateTime(log.occurred_at, lang)}
                    </TableCell>
                    <TableCell className="text-xs font-mono">{log.endpoint}</TableCell>
                    <TableCell>
                      <Badge variant={log.outcome === 'SUCCESS' ? 'success' : 'destructive'} className="text-xs">
                        {outcomeLabels[log.outcome] ?? log.outcome}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <Badge variant={SEVERITY_BADGE[log.severity]} className="text-xs">
                        {severityLabels[log.severity] ?? log.severity}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-xs text-muted-foreground">
                      {log.response_time_ms != null ? formatMessage(dict.integrationsHealth.summary.msUnit, { value: log.response_time_ms }) : '—'}
                    </TableCell>
                    <TableCell className="text-xs text-muted-foreground max-w-64">
                      {log.failure_type ? (failureTypeLabels[log.failure_type] ?? log.failure_type) : '—'}
                      {log.impact ? ` · ${log.impact}` : ''}
                    </TableCell>
                    <TableCell className="pr-6 text-xs text-muted-foreground">{log.action_taken ?? '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : (
            <p className="px-6 py-8 text-center text-sm text-muted-foreground">{dict.integrationsHealth.table.empty}</p>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
