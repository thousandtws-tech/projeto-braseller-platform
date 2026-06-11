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
} from '@/shared/ui/dialog'
import { Button, buttonVariants } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { authenticateAction } from '@/features/connectors/server/actions'
import { formatMessage } from '@/shared/i18n/format'
import type { Dictionary } from '@/shared/i18n/get-dictionary'

const ML_OAUTH_URL =
  'https://auth.mercadolivre.com.br/authorization?response_type=code' +
  '&client_id=4587994283757685' +
  '&redirect_uri=https%3A%2F%2Fgateway-api.salmonrock-4d3f2812.brazilsouth.azurecontainerapps.io%2Fintegrations%2Fmercado-livre%2Fcallback'

interface Marketplace {
  name: string
  displayName: string
  bg: string
  initials: string
  iconSrc?: string
  iconAlt?: string
  oauthUrl?: string
  fields: { name: string; label: string; placeholder: string; type?: string; hint?: string }[]
}

function getMarketplaces(dict: Dictionary): Marketplace[] {
  return [
    {
      name: 'shopee',
      displayName: 'Shopee',
      bg: 'bg-orange-500',
      initials: 'SP',
      iconSrc: '/favicons/favicon.ico',
      iconAlt: 'Shopee',
      fields: [
        { name: 'shop_id', ...dict.connectors.fields.shopId },
        { name: 'code', ...dict.connectors.fields.shopeeCode },
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
        { name: 'client_id', ...dict.connectors.fields.clientId },
        { name: 'client_secret', ...dict.connectors.fields.clientSecret, type: 'password' },
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
        { name: 'api_key', ...dict.connectors.fields.blingApiKey, type: 'password' },
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
        { name: 'seller_id', ...dict.connectors.fields.sellerId },
        { name: 'refresh_token', ...dict.connectors.fields.refreshToken, type: 'password' },
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
        { name: 'api_key', ...dict.connectors.fields.olistApiKey, type: 'password' },
      ],
    },
  ]
}

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  existingConnectors?: string[]
  dict: Dictionary
}

export function AddMarketplaceDialog({ open, onOpenChange, existingConnectors = [], dict }: Props) {
  const [selected, setSelected] = useState<Marketplace | null>(null)
  const [state, formAction, isPending] = useActionState(authenticateAction, null)

  function handleClose(o: boolean) {
    if (!o) {
      setSelected(null)
    }
    onOpenChange(o)
  }

  const marketplaces = getMarketplaces(dict)
  const available = marketplaces.filter(
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
                <DialogTitle>{formatMessage(dict.connectors.addMarketplaceDialog.connectTitle, { displayName: selected.displayName })}</DialogTitle>
              </div>
              <DialogDescription>
                {dict.connectors.addMarketplaceDialog.credentialDescription}
              </DialogDescription>
            </>
          ) : (
            <>
              <DialogTitle>{dict.connectors.addMarketplaceDialog.chooseTitle}</DialogTitle>
              <DialogDescription>
                {dict.connectors.addMarketplaceDialog.chooseDescription}
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
                {dict.connectors.addMarketplaceDialog.allConnected}
              </p>
            )}
          </div>
        )}

        {/* Etapa 2 — OAuth ou Credenciais */}
        {selected && (
          selected.oauthUrl ? (
            <div className="space-y-4">
              <div className="rounded-lg border border-border bg-muted/40 p-4 text-sm text-muted-foreground space-y-2">
                <p>
                  {dict.connectors.addMarketplaceDialog.oauthIntroPrefix}{' '}
                  <strong className="text-foreground">{dict.connectors.addMarketplaceDialog.oauthIntroHighlight}</strong>
                  {formatMessage(dict.connectors.addMarketplaceDialog.oauthIntroSuffix, { displayName: selected.displayName })}
                </p>
                <p>{dict.connectors.addMarketplaceDialog.oauthNote}</p>
              </div>
              <DialogFooter className="border-0 bg-transparent p-0 mt-2">
                <Button variant="outline" onClick={() => handleClose(false)}>{dict.connectors.dialog.cancel}</Button>
                <a href={selected.oauthUrl} className={buttonVariants()}>
                  <Plug className="size-4" />
                  {formatMessage(dict.connectors.addMarketplaceDialog.authorizeWith, { displayName: selected.displayName })}
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
                  <span>{formatMessage(dict.connectors.dialog.success, { displayName: selected.displayName })}</span>
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
                  {dict.connectors.dialog.cancel}
                </Button>
                <Button type="submit" disabled={isPending || state?.success === true}>
                  {isPending
                    ? <><Loader2 className="size-4 animate-spin" />{dict.connectors.dialog.connecting}</>
                    : <><Plug className="size-4" />{dict.connectors.dialog.connect}</>
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
