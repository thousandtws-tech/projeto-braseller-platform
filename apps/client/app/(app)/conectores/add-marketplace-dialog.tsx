'use client'

import { useActionState, useState } from 'react'
import Image from 'next/image'
import { Loader2, AlertCircle, CheckCircle2, Plug, ChevronLeft } from 'lucide-react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog'
import { Button, buttonVariants } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { authenticateAction } from '@/app/actions/connectors'

const ML_OAUTH_URL =
  'https://auth.mercadolivre.com.br/authorization?response_type=code' +
  '&client_id=4587994283757685' +
  '&redirect_uri=https%3A%2F%2Fgateway-api.salmonrock-4d3f2812.brazilsouth.azurecontainerapps.io%2Fintegrations%2Fmercado-livre%2Fcallback'

const MARKETPLACES: {
  name: string
  displayName: string
  bg: string
  initials: string
  iconSrc?: string
  iconAlt?: string
  oauthUrl?: string
  fields: { name: string; label: string; placeholder: string; type?: string }[]
}[] = [
  {
    name: 'shopee',
    displayName: 'Shopee',
    bg: 'bg-orange-500',
    initials: 'SP',
    iconSrc: '/favicons/favicon.ico',
    iconAlt: 'Shopee',
    fields: [
      { name: 'partner_id',  label: 'Partner ID',  placeholder: 'ID do parceiro' },
      { name: 'partner_key', label: 'Partner Key', placeholder: 'Chave do parceiro', type: 'password' },
      { name: 'shop_id',     label: 'Shop ID',     placeholder: 'ID da loja' },
    ],
  },
  {
    name: 'magalu',
    displayName: 'Magalu',
    bg: 'bg-blue-500',
    initials: 'MG',
    iconSrc: '/favicons/magalu.ico',
    iconAlt: 'Magalu',
    fields: [
      { name: 'client_id',     label: 'Client ID',     placeholder: 'Client ID Magalu' },
      { name: 'client_secret', label: 'Client Secret', placeholder: 'Client Secret', type: 'password' },
    ],
  },
  {
    name: 'bling',
    displayName: 'Bling',
    bg: 'bg-indigo-500',
    initials: 'BL',
    iconSrc: '/favicons/bling.ico',
    iconAlt: 'Bling',
    fields: [
      { name: 'api_key', label: 'API Key', placeholder: 'Chave de API do Bling', type: 'password' },
    ],
  },
  {
    name: 'amazon',
    displayName: 'Amazon',
    bg: 'bg-blue-700',
    initials: 'AZ',
    iconSrc: '/favicons/amazon.ico',
    iconAlt: 'Amazon',
    fields: [
      { name: 'seller_id',     label: 'Seller ID',     placeholder: 'ID do vendedor Amazon' },
      { name: 'client_id',     label: 'LWA Client ID', placeholder: 'Login with Amazon Client ID' },
      { name: 'client_secret', label: 'LWA Secret',    placeholder: 'LWA Secret', type: 'password' },
      { name: 'refresh_token', label: 'Refresh Token', placeholder: 'Token de atualização', type: 'password' },
    ],
  },
  {
    name: 'mercado-livre',
    displayName: 'Mercado Livre',
    bg: 'bg-yellow-400',
    initials: 'ML',
    iconSrc: '/favicons/180x180.png',
    iconAlt: 'Mercado Livre',
    oauthUrl: ML_OAUTH_URL,
    fields: [],
  },
  {
    name: 'olist',
    displayName: 'Olist',
    bg: 'bg-yellow-600',
    initials: 'OL',
    iconSrc: '/favicons/olist.ico',
    iconAlt: 'Olist',
    fields: [
      { name: 'api_key', label: 'API Key', placeholder: 'Chave de API Olist', type: 'password' },
    ],
  },
]

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  existingConnectors?: string[]
}

export function AddMarketplaceDialog({ open, onOpenChange, existingConnectors = [] }: Props) {
  const [selected, setSelected] = useState<typeof MARKETPLACES[0] | null>(null)
  const [state, formAction, isPending] = useActionState(authenticateAction, null)

  function handleClose(o: boolean) {
    if (!o) {
      setSelected(null)
    }
    onOpenChange(o)
  }

  const available = MARKETPLACES.filter(
    (m) => !existingConnectors.includes(m.name)
  )

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          {selected ? (
            <>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setSelected(null)}
                  className="p-1 rounded hover:bg-muted transition-colors text-muted-foreground hover:text-foreground"
                >
                  <ChevronLeft className="size-4" />
                </button>
                <DialogTitle>Conectar {selected.displayName}</DialogTitle>
              </div>
              <DialogDescription>
                Informe as credenciais da sua conta para autorizar a integração.
              </DialogDescription>
            </>
          ) : (
            <>
              <DialogTitle>Adicionar Marketplace</DialogTitle>
              <DialogDescription>
                Escolha a plataforma que deseja integrar.
              </DialogDescription>
            </>
          )}
        </DialogHeader>

        {/* Etapa 1 — Seleção */}
        {!selected && (
          <div className="grid grid-cols-3 gap-3 py-2">
            {available.map((m) => (
              <button
                key={m.name}
                type="button"
                onClick={() => setSelected(m)}
                className="flex flex-col items-center gap-2 rounded-xl border border-border p-4 hover:border-primary/50 hover:bg-accent/50 transition-colors group"
              >
                <div className={`size-10 overflow-hidden rounded-lg ${m.bg} flex items-center justify-center`}>
                  {m.iconSrc ? (
                    <Image
                      src={m.iconSrc}
                      alt={m.iconAlt ?? m.displayName}
                      width={40}
                      height={40}
                      unoptimized
                      className="size-full object-cover"
                    />
                  ) : (
                    <span className="text-sm font-bold text-white">{m.initials}</span>
                  )}
                </div>
                <span className="text-xs font-medium text-center leading-tight group-hover:text-primary transition-colors">
                  {m.displayName}
                </span>
              </button>
            ))}
            {available.length === 0 && (
              <p className="col-span-3 text-center text-sm text-muted-foreground py-4">
                Todos os marketplaces disponíveis já estão conectados.
              </p>
            )}
          </div>
        )}

        {/* Etapa 2 — OAuth ou Credenciais */}
        {selected && (
          selected.oauthUrl ? (
            <div className="space-y-4">
              <div className="rounded-lg border border-border bg-muted/40 p-4 text-sm text-muted-foreground space-y-2">
                <p>Ao clicar em <strong className="text-foreground">Autorizar</strong>, você será redirecionado para o site do {selected.displayName}.</p>
                <p>Após aprovar, o acesso será configurado automaticamente.</p>
              </div>
              <DialogFooter className="border-0 bg-transparent p-0 mt-2">
                <Button variant="outline" onClick={() => handleClose(false)}>Cancelar</Button>
                <a href={selected.oauthUrl} className={buttonVariants()}>
                  <Plug className="size-4" />
                  Autorizar com {selected.displayName}
                </a>
              </DialogFooter>
            </div>
          ) : (
            <form action={formAction} className="space-y-4">
              <input type="hidden" name="connectorName" value={selected.name} />

              {state?.success === false && (
                <div className="flex items-start gap-2 rounded-lg border border-destructive/30 bg-destructive/8 px-3 py-2.5 text-sm text-destructive">
                  <AlertCircle className="size-4 mt-0.5 shrink-0" />
                  <span>{state.error}</span>
                </div>
              )}
              {state?.success === true && (
                <div className="flex items-start gap-2 rounded-lg border border-emerald-500/30 bg-emerald-500/8 px-3 py-2.5 text-sm text-emerald-700 dark:text-emerald-400">
                  <CheckCircle2 className="size-4 mt-0.5 shrink-0" />
                  <span>{selected.displayName} conectado com sucesso!</span>
                </div>
              )}

              {selected.fields.map((field) => (
                <div key={field.name} className="space-y-1.5">
                  <Label htmlFor={`add-${field.name}`}>{field.label}</Label>
                  <Input
                    id={`add-${field.name}`}
                    name={field.name}
                    type={field.type ?? 'text'}
                    placeholder={field.placeholder}
                    required
                    disabled={isPending || state?.success === true}
                    autoComplete="off"
                  />
                </div>
              ))}

              <DialogFooter className="border-0 bg-transparent p-0 mt-2">
                <Button type="button" variant="outline" onClick={() => handleClose(false)} disabled={isPending}>
                  Cancelar
                </Button>
                <Button type="submit" disabled={isPending || state?.success === true}>
                  {isPending
                    ? <><Loader2 className="size-4 animate-spin" />Conectando...</>
                    : <><Plug className="size-4" />Conectar</>
                  }
                </Button>
              </DialogFooter>
            </form>
          )
        )}
      </DialogContent>
    </Dialog>
  )
}
