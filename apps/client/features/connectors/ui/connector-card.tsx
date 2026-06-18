'use client'

import { useActionState, useState } from 'react'
import {
  AlertCircle,
  CheckCircle,
  Clock,
  Loader2,
  LockKeyhole,
  Plug,
  RefreshCw,
  XCircle,
} from 'lucide-react'

import { syncAllAction } from '@/features/connectors/server/actions'
import { ReadOnlyLock } from '@/shared/ui/read-only-lock'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { formatMessage } from '@/shared/i18n/format'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'
import type { ConnectorStatus } from '@/shared/types'

import { ConnectDialog } from './connect-dialog'
import { MarketplaceLogo } from './marketplace-brand'

const LOCALE_MAP: Record<Locale, string> = { 'pt-BR': 'pt-BR', en: 'en-US', es: 'es-ES' }

const STATUS_ICONS = {
  connected: { icon: CheckCircle, className: 'text-emerald-600 dark:text-emerald-400' },
  disconnected: { icon: XCircle, className: 'text-muted-foreground' },
  error: { icon: AlertCircle, className: 'text-destructive' },
  syncing: { icon: RefreshCw, className: 'text-primary' },
}

const JOB_STATUS_VARIANT: Record<string, 'success' | 'warning' | 'secondary' | 'destructive'> = {
  QUEUED: 'secondary',
  PROCESSING: 'warning',
  COMPLETE: 'success',
  ERROR: 'destructive',
}

interface Props {
  connector: ConnectorStatus
  readOnly?: boolean
  dict: Dictionary
  lang: Locale
}

export function ConnectorCard({ connector, readOnly = false, dict, lang }: Props) {
  const [syncState, syncFormAction, isSyncing] = useActionState(syncAllAction, null)
  const [dialogOpen, setDialogOpen] = useState(false)

  const safeName = connector.name ?? ''
  const isConnected = connector.status === 'connected'
  const statusLabels = dict.connectors.status as Record<string, string>
  const jobStatusLabels = dict.connectors.jobStatus as Record<string, string>
  const status = STATUS_ICONS[connector.status] ?? STATUS_ICONS.disconnected
  const StatusIcon = status.icon
  const label = statusLabels[connector.status] ?? statusLabels.disconnected

  return (
    <>
      <Card className={isConnected ? '' : 'border-dashed'}>
        <CardHeader className="flex-row items-center gap-3">
          <MarketplaceLogo
            name={safeName}
            displayName={connector.displayName}
            className="size-10"
          />
          <div className="min-w-0 flex-1">
            <h3 className="truncate text-sm font-semibold">{connector.displayName}</h3>
            <div className={`mt-0.5 flex items-center gap-1 text-xs ${status.className}`}>
              <StatusIcon className="size-3 shrink-0" />
              {label}
            </div>
          </div>
        </CardHeader>

        <CardContent className="flex flex-col gap-4">
          {readOnly && <ReadOnlyLock compact />}

          {connector.lastSync && (
            <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
              <Clock className="size-3 shrink-0" />
              {new Date(connector.lastSync).toLocaleString(LOCALE_MAP[lang], {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              })}
            </div>
          )}

          {syncState?.success === true && (
            <div className="space-y-1.5 rounded-md border border-border bg-muted/40 px-3 py-2 text-xs">
              <div className="flex items-center justify-between">
                <span className="font-medium">{dict.connectors.card.syncResult}</span>
                <Badge
                  variant={JOB_STATUS_VARIANT[syncState.job.status] ?? 'secondary'}
                  className="text-[10px]"
                >
                  {jobStatusLabels[syncState.job.status] ?? syncState.job.status}
                </Badge>
              </div>
              <div className="grid grid-cols-3 gap-1 text-muted-foreground">
                <span>{formatMessage(dict.connectors.card.orders, { count: syncState.job.orders_synced })}</span>
                <span>{formatMessage(dict.connectors.card.payments, { count: syncState.job.payments_synced })}</span>
                <span>{formatMessage(dict.connectors.card.fees, { count: syncState.job.fees_synced })}</span>
              </div>
              {syncState.job.error_message && (
                <p className="text-destructive">{syncState.job.error_message}</p>
              )}
            </div>
          )}

          {syncState?.success === false && (
            <div className="rounded-md border border-destructive/20 bg-destructive/10 px-3 py-2 text-xs text-destructive">
              {syncState.error}
            </div>
          )}

          {isConnected ? (
            <form action={syncFormAction}>
              <input type="hidden" name="connectorName" value={safeName} />
              <Button
                type="submit"
                size="lg"
                variant="outline"
                className="w-full"
                disabled={readOnly || isSyncing}
              >
                {isSyncing ? (
                  <>
                    <Loader2 className="size-3.5 animate-spin" />
                    {dict.connectors.card.syncingButton}
                  </>
                ) : readOnly ? (
                  <>
                    <LockKeyhole className="size-3.5 animate-pulse" />
                    {dict.connectors.card.sync}
                  </>
                ) : (
                  <>
                    <RefreshCw className="size-3.5" />
                    {dict.connectors.card.sync}
                  </>
                )}
              </Button>
            </form>
          ) : (
            <Button
              size="lg"
              className="w-full"
              disabled={readOnly}
              onClick={() => setDialogOpen(true)}
            >
              {readOnly ? (
                <LockKeyhole className="size-3.5 animate-pulse" />
              ) : (
                <Plug className="size-3.5" />
              )}
              {dict.connectors.card.connect}
            </Button>
          )}
        </CardContent>
      </Card>

      {dialogOpen ? (
        <ConnectDialog
          connectorName={safeName}
          displayName={connector.displayName}
          open={dialogOpen}
          onOpenChange={setDialogOpen}
          dict={dict}
          lang={lang}
        />
      ) : null}
    </>
  )
}
