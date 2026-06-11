'use client'

import { useActionState, useState, useEffect } from 'react'
import Image from 'next/image'
import { RefreshCw, CheckCircle, XCircle, AlertCircle, Loader2, ExternalLink, Clock, Plug, LockKeyhole } from 'lucide-react'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { syncAllAction } from '@/features/connectors/server/actions'
import { ReadOnlyLock } from '@/shared/ui/read-only-lock'
import { formatMessage } from '@/shared/i18n/format'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'
import { ConnectDialog } from './connect-dialog'
import type { ConnectorStatus } from '@/shared/types'

const LOCALE_MAP: Record<Locale, string> = { 'pt-BR': 'pt-BR', en: 'en-US', es: 'es-ES' }

const ML_FAVICON = '/favicons/180x180.png'
const SHOPEE_FAVICON = '/favicons/favicon.ico'
const MAGALU_FAVICON = '/favicons/magalu.ico'
const BLING_FAVICON = '/favicons/bling.ico'
const AMAZON_FAVICON = '/favicons/amazon.ico'
const OLIST_FAVICON = '/favicons/olist.ico'

const PLATFORM_LOGOS: Record<string, { bg: string; initials: string; src?: string; alt?: string }> = {
  mercadolivre:    { bg: 'bg-yellow-400', initials: 'ML', src: ML_FAVICON, alt: 'Mercado Livre' },
  'mercado-livre': { bg: 'bg-yellow-400', initials: 'ML', src: ML_FAVICON, alt: 'Mercado Livre' },
  mercadolibre:    { bg: 'bg-yellow-400', initials: 'ML', src: ML_FAVICON, alt: 'Mercado Livre' },
  shopee:          { bg: 'bg-orange-500', initials: 'SP', src: SHOPEE_FAVICON, alt: 'Shopee' },
  amazon:          { bg: 'bg-blue-600',   initials: 'AZ', src: AMAZON_FAVICON, alt: 'Amazon' },
  magalu:          { bg: 'bg-blue-500',   initials: 'MG', src: MAGALU_FAVICON, alt: 'Magalu' },
  bling:           { bg: 'bg-indigo-500', initials: 'BL', src: BLING_FAVICON, alt: 'Bling' },
  olist:           { bg: 'bg-yellow-600', initials: 'OL', src: OLIST_FAVICON, alt: 'Olist' },
}

const STATUS_ICONS = {
  connected: { icon: CheckCircle, className: 'text-emerald-600 dark:text-emerald-400' },
  disconnected: { icon: XCircle, className: 'text-muted-foreground' },
  error: { icon: AlertCircle, className: 'text-destructive' },
  syncing: { icon: RefreshCw, className: 'text-primary' },
}

const JOB_STATUS_VARIANT: Record<string, 'success' | 'warning' | 'secondary' | 'destructive'> = {
  QUEUED: 'secondary', PROCESSING: 'warning', COMPLETE: 'success', ERROR: 'destructive',
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

  // Fecha o dialog e recarrega a página após conexão bem-sucedida
  useEffect(() => {
    if (!dialogOpen) return
    // O ConnectDialog cuida do próprio fechamento; aqui apenas recarregamos após fechar
  }, [dialogOpen])

  const safeName = connector.name ?? ''
  const logo = PLATFORM_LOGOS[safeName] ?? {
    bg: 'bg-muted',
    initials: safeName.slice(0, 2).toUpperCase() || '??',
  }
  const isConnected = connector.status === 'connected'
  const statusLabels = dict.connectors.status as Record<string, string>
  const jobStatusLabels = dict.connectors.jobStatus as Record<string, string>
  const { icon: StatusIcon, className } = STATUS_ICONS[connector.status] ?? STATUS_ICONS.disconnected
  const label = statusLabels[connector.status] ?? statusLabels.disconnected

  return (
    <>
      <Card className={isConnected ? '' : 'opacity-75'}>
        <CardHeader className="flex-row items-center gap-3 pb-3">
          <div className={`size-10 overflow-hidden rounded-lg ${logo.bg} flex items-center justify-center shrink-0`}>
            {logo.src ? (
              <Image
                src={logo.src}
                alt={logo.alt ?? connector.displayName}
                width={40}
                height={40}
                unoptimized
                className="size-full object-cover"
              />
            ) : (
              <span className="text-sm font-bold text-white">{logo.initials}</span>
            )}
          </div>
          <div className="flex-1 min-w-0">
            <h3 className="font-semibold text-sm truncate">{connector.displayName}</h3>
            <div className={`flex items-center gap-1 text-xs mt-0.5 ${className}`}>
              <StatusIcon className="size-3 shrink-0" />
              {label}
            </div>
          </div>
        </CardHeader>

        <CardContent className="space-y-3">
          {readOnly && <ReadOnlyLock compact />}

          {connector.lastSync && (
            <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
              <Clock className="size-3 shrink-0" />
              {new Date(connector.lastSync).toLocaleString(LOCALE_MAP[lang], {
                day: '2-digit', month: '2-digit', year: 'numeric',
                hour: '2-digit', minute: '2-digit',
              })}
            </div>
          )}

          {syncState?.success === true && (
            <div className="rounded-md border border-border bg-muted/40 px-3 py-2 text-xs space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="font-medium">{dict.connectors.card.syncResult}</span>
                <Badge variant={JOB_STATUS_VARIANT[syncState.job.status] ?? 'secondary'} className="text-[10px]">
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
            <div className="rounded-md bg-destructive/10 border border-destructive/20 px-3 py-2 text-xs text-destructive">
              {syncState.error}
            </div>
          )}

          {isConnected ? (
            <form action={syncFormAction} className="flex gap-2">
              <input type="hidden" name="connectorName" value={safeName} />
              <Button type="submit" size="sm" variant="outline" className="flex-1" disabled={readOnly || isSyncing}>
                {isSyncing
                  ? <><Loader2 className="size-3.5 animate-spin" />{dict.connectors.card.syncingButton}</>
                  : readOnly
                    ? <><LockKeyhole className="size-3.5 animate-pulse" />{dict.connectors.card.sync}</>
                    : <><RefreshCw className="size-3.5" />{dict.connectors.card.sync}</>
                }
              </Button>
              <Button type="button" size="sm" variant="ghost">
                <ExternalLink className="size-3.5" />
              </Button>
            </form>
          ) : (
            <Button
              size="sm"
              className="w-full"
              disabled={readOnly}
              onClick={() => setDialogOpen(true)}
            >
              {readOnly ? <LockKeyhole className="size-3.5 animate-pulse" /> : <Plug className="size-3.5" />}
              {dict.connectors.card.connect}
            </Button>
          )}
        </CardContent>
      </Card>

      <ConnectDialog
        connectorName={safeName}
        displayName={connector.displayName}
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        dict={dict}
        lang={lang}
      />
    </>
  )
}
