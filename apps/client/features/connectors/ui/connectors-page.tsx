import type { Metadata } from 'next'
import Image from 'next/image'
import { AlertCircle, CheckCircle2, Clock, Wifi } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Badge } from '@/shared/ui/badge'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/shared/ui/table'
import { getToken, getSession } from '@/entities/session/server/session'
import { getConnectors } from '@/shared/api/gateway'
import { isReadOnlyAccountant } from '@/entities/session/model/permissions'
import { ConnectorCard } from './connector-card'
import { AddMarketplaceCard } from './add-marketplace-card'

export const metadata: Metadata = { title: 'Conectores' }

const DISPLAY_NAMES: Record<string, string> = {
  'mercado-livre': 'Mercado Livre',
  shopee: 'Shopee',
  amazon: 'Amazon',
  magalu: 'Magalu',
  bling: 'Bling',
}

interface Props {
  searchParams: Promise<{ connected?: string; auth_error?: string }>
}

export default async function ConectoresPage({ searchParams }: Props) {
  const { connected: connectedParam, auth_error } = await searchParams
  const token = (await getToken()) ?? ''
  const session = await getSession()
  const readOnly = isReadOnlyAccountant(session?.roles)
  const connectors = await getConnectors(token)

  const connectedCount = connectors.filter((c) => c.status === 'connected').length

  return (
    <div className="space-y-6 max-w-5xl">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-xl font-semibold">Conectores</h2>
          <p className="text-sm text-muted-foreground">
            {connectedCount} de {connectors.length} marketplaces conectados
          </p>
        </div>
      </div>

      {/* Feedback do OAuth callback */}
      {connectedParam && (
        <div className="rounded-lg border border-emerald-500/30 bg-emerald-500/5 p-4 flex items-center gap-3">
          <CheckCircle2 className="size-4 text-emerald-600 shrink-0" />
          <p className="text-sm font-medium">
            {DISPLAY_NAMES[connectedParam] ?? connectedParam} conectado com sucesso!
          </p>
        </div>
      )}
      {auth_error && (
        <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 flex items-center gap-3">
          <AlertCircle className="size-4 text-destructive shrink-0" />
          <p className="text-sm font-medium">{decodeURIComponent(auth_error)}</p>
        </div>
      )}

      {connectedCount < connectors.length && connectors.length > 0 && !auth_error && !connectedParam && (
        <div className="rounded-lg border border-amber-500/30 bg-amber-500/5 p-4 flex items-center gap-3">
          <AlertCircle className="size-4 text-amber-500 shrink-0" />
          <div className="flex-1">
            <p className="text-sm font-medium">Alguns conectores precisam de atenção</p>
            <p className="text-xs text-muted-foreground mt-0.5">
              Verifique os conectores desconectados ou com erro abaixo.
            </p>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {connectors.map((connector) => (
          <ConnectorCard key={connector.name} connector={connector} readOnly={readOnly} />
        ))}
        <AddMarketplaceCard existingConnectors={connectors.map((c) => c.name)} readOnly={readOnly} />
      </div>

      {/* Tabela de conectores ativos */}
      {connectors.length > 0 && (
        <Card>
          <CardHeader className="flex-row items-center gap-3 pb-3">
            <div className="size-8 rounded-lg bg-primary/10 flex items-center justify-center">
              <Wifi className="size-4 text-primary" />
            </div>
            <CardTitle>Conectores ativos</CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="pl-6">Marketplace</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Última sincronização</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {connectors.map((c) => (
                  <TableRow key={c.name}>
                    <TableCell className="pl-6">
                      <div className="flex items-center gap-2.5">
                        <div className={`size-7 overflow-hidden rounded-md flex items-center justify-center text-[10px] font-bold text-white ${
                          LOGO_BG[c.name] ?? 'bg-muted'
                        }`}>
                          {LOGO_SRC[c.name] ? (
                            <Image
                              src={LOGO_SRC[c.name]}
                              alt={c.displayName}
                              width={28}
                              height={28}
                              unoptimized
                              className="size-full object-cover"
                            />
                          ) : (
                            LOGO_INITIALS[c.name] ?? c.name.slice(0, 2).toUpperCase()
                          )}
                        </div>
                        <span className="text-sm font-medium">{c.displayName}</span>
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge
                        variant={c.status === 'connected' ? 'success' : c.status === 'error' ? 'destructive' : 'secondary'}
                        className="text-xs"
                      >
                        {c.status === 'connected' ? 'Conectado' : c.status === 'error' ? 'Erro' : c.status === 'syncing' ? 'Sincronizando' : 'Desconectado'}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      {c.lastSync ? (
                        <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                          <Clock className="size-3" />
                          {new Date(c.lastSync).toLocaleString('pt-BR', {
                            day: '2-digit', month: '2-digit', year: 'numeric',
                            hour: '2-digit', minute: '2-digit',
                          })}
                        </div>
                      ) : (
                        <span className="text-xs text-muted-foreground">—</span>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

const LOGO_BG: Record<string, string> = {
  mercadolivre: 'bg-yellow-400', 'mercado-livre': 'bg-yellow-400',
  mercadolibre: 'bg-yellow-400',
  shopee: 'bg-orange-500', amazon: 'bg-blue-600',
  magalu: 'bg-blue-500', bling: 'bg-indigo-500',
}
const LOGO_INITIALS: Record<string, string> = {
  mercadolivre: 'ML', 'mercado-livre': 'ML', mercadolibre: 'ML',
  shopee: 'SP', amazon: 'AZ', magalu: 'MG', bling: 'BL',
}
const LOGO_SRC: Record<string, string> = {
  mercadolivre: '/favicons/180x180.png',
  'mercado-livre': '/favicons/180x180.png',
  mercadolibre: '/favicons/180x180.png',
  shopee: '/favicons/favicon.ico',
  amazon: '/favicons/amazon.ico',
  magalu: '/favicons/magalu.ico',
  bling: '/favicons/bling.ico',
  olist: '/favicons/olist.ico',
}
