import { CheckCircle2, Clock3, Plug, RefreshCw, Unplug } from 'lucide-react'

import { Badge } from '@/shared/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/shared/ui/table'
import { getToken, getSession } from '@/entities/session/server/session'
import { getConnectors } from '@/shared/api/gateway'
import { isReadOnlyAccountant } from '@/entities/session/model/permissions'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import { formatMessage } from '@/shared/i18n/format'
import type { Locale } from '@/shared/i18n/config'
import { ConnectorCard } from './connector-card'
import { AddMarketplaceCard } from './add-marketplace-card'
import { MarketplaceLogo } from './marketplace-brand'
import { OAuthResultFeedback } from './oauth-result-feedback'

const LOCALE_MAP: Record<Locale, string> = { 'pt-BR': 'pt-BR', en: 'en-US', es: 'es-ES' }
const DISPLAY_NAMES: Record<string, string> = {
  'mercado-livre': 'Mercado Livre',
  shopee: 'Shopee',
  amazon: 'Amazon',
  magalu: 'Magalu',
  bling: 'Bling',
}

interface Props {
  params: Promise<{ lang: Locale }>
  searchParams: Promise<{ connected?: string; auth_error?: string }>
}

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.connectors.title }
}

export default async function ConectoresPage({ params, searchParams }: Props) {
  const { lang } = await params
  const [dict, query, token, session] = await Promise.all([
    getDictionary(lang),
    searchParams,
    getToken().then((value) => value ?? ''),
    getSession(),
  ])
  const readOnly = isReadOnlyAccountant(session?.roles)
  const connectors = await getConnectors(token)
  const connected = connectors.filter((item) => item.status === 'connected')
  const attention = connectors.filter((item) => item.status === 'error' || item.status === 'disconnected')
  const syncing = connectors.filter((item) => item.status === 'syncing')
  const statusLabels = dict.connectors.status as Record<string, string>
  const authErrorMessages: Record<string, string> = {
    access_denied: dict.connectors.callback.accessDenied,
    invalid_state: dict.connectors.callback.invalidState,
    missing_code: dict.connectors.callback.missingCode,
    oauth_not_configured: dict.connectors.callback.notConfigured,
    service_unavailable: dict.connectors.callback.serviceUnavailable,
    authentication_failed: dict.connectors.callback.authError,
    invalid_locale: dict.connectors.callback.authError,
  }
  const successMessage = query.connected
    ? formatMessage(dict.connectors.connectedSuccess, {
        name: DISPLAY_NAMES[query.connected] ?? query.connected,
      })
    : undefined
  const errorMessage = query.auth_error
    ? authErrorMessages[query.auth_error] ?? dict.connectors.callback.authError
    : undefined

  return (
    <div className="flex w-full flex-col gap-6">
      <header>
        <h2 className="text-[1.8rem] font-semibold tracking-[-0.04em]">{dict.connectors.title}</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Centralize canais de venda e controle a atualização dos dados importados.
        </p>
      </header>

      <section className="grid grid-cols-2 overflow-hidden rounded-lg border border-border bg-card xl:grid-cols-4">
        <Metric label="Conectados" value={connected.length} helper={`${connectors.length} disponíveis`} icon={Plug} />
        <Metric label="Precisam de atenção" value={attention.length} helper="Desconectados ou com erro" icon={Unplug} />
        <Metric label="Sincronizando" value={syncing.length} helper="Processos em andamento" icon={RefreshCw} />
        <Metric label="Cobertura" value={connectors.length > 0 ? Math.round((connected.length / connectors.length) * 100) : 0} helper="Dos canais cadastrados" icon={CheckCircle2} suffix="%" />
      </section>

      <OAuthResultFeedback successMessage={successMessage} errorMessage={errorMessage} />

      <section>
        <div className="mb-3 flex items-end justify-between gap-4">
          <div><h3 className="text-sm font-semibold">Canais</h3><p className="mt-1 text-xs text-muted-foreground">Conecte, sincronize e acompanhe cada integração.</p></div>
          {attention.length > 0 ? <Badge variant="warning">{attention.length} requerem ação</Badge> : null}
        </div>
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {connectors.map((connector) => (
            <ConnectorCard key={connector.name} connector={connector} readOnly={readOnly} dict={dict} lang={lang} />
          ))}
          <AddMarketplaceCard existingConnectors={connectors.map((item) => item.name)} readOnly={readOnly} dict={dict} lang={lang} />
        </div>
      </section>

      <Card className="overflow-hidden">
        <CardHeader><CardTitle>Histórico de sincronização</CardTitle><p className="text-xs text-muted-foreground">Última atividade conhecida em cada conector.</p></CardHeader>
        <CardContent className="p-0">
          {connectors.length === 0 ? (
            <div className="flex min-h-52 flex-col items-center justify-center gap-3 text-center"><Plug className="size-6 text-muted-foreground" /><p className="text-sm font-medium">Nenhum conector cadastrado</p><p className="text-xs text-muted-foreground">Adicione seu primeiro marketplace para começar.</p></div>
          ) : (
            <Table>
              <TableHeader className="bg-muted/70"><TableRow className="hover:bg-muted/70"><TableHead className="pl-5">Marketplace</TableHead><TableHead>Status</TableHead><TableHead className="pr-5">Última sincronização</TableHead></TableRow></TableHeader>
              <TableBody>
                {connectors.map((connector) => (
                  <TableRow key={connector.name}>
                    <TableCell className="pl-5 font-medium">
                      <span className="flex items-center gap-3">
                        <MarketplaceLogo
                          name={connector.name}
                          displayName={connector.displayName}
                          className="size-8 rounded-md"
                          imageClassName="p-1"
                        />
                        {connector.displayName}
                      </span>
                    </TableCell>
                    <TableCell><Badge variant={connector.status === 'connected' ? 'secondary' : connector.status === 'error' ? 'destructive' : 'outline'}>{statusLabels[connector.status] ?? connector.status}</Badge></TableCell>
                    <TableCell className="pr-5 text-xs text-muted-foreground">
                      <span className="flex items-center gap-2"><Clock3 className="size-3.5" />{connector.lastSync ? new Date(connector.lastSync).toLocaleString(LOCALE_MAP[lang]) : 'Ainda não sincronizado'}</span>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

function Metric({ label, value, helper, icon: Icon, suffix = '' }: { label: string; value: number; helper: string; icon: React.ComponentType<{ className?: string }>; suffix?: string }) {
  return <div className="flex min-h-32 flex-col justify-between gap-3 border-b border-r border-border p-5 even:border-r-0 [&:nth-last-child(-n+2)]:border-b-0 xl:min-h-28 xl:border-b-0 xl:even:border-r xl:last:border-r-0"><div className="flex items-center justify-between"><span className="text-xs text-muted-foreground">{label}</span><Icon className="size-4 text-muted-foreground" /></div><p className="text-2xl font-semibold tracking-[-0.035em] tabular-nums">{value}{suffix}</p><p className="text-[11px] text-muted-foreground">{helper}</p></div>
}
