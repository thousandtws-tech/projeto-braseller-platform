'use client'

import { useActionState, useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import {
  AlertCircle,
  ArrowLeft,
  ArrowRight,
  Check,
  CheckCircle2,
  ExternalLink,
  Loader2,
  LockKeyhole,
  Plug,
  ShieldCheck,
} from 'lucide-react'

import { authenticateAction } from '@/features/connectors/server/actions'
import { Button, buttonVariants } from '@/shared/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/shared/ui/dialog'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { formatMessage } from '@/shared/i18n/format'
import type { Dictionary } from '@/shared/i18n/get-dictionary'

import { MarketplaceLogo, normalizeMarketplaceName } from './marketplace-brand'

const ML_OAUTH_URL =
  'https://auth.mercadolivre.com.br/authorization?response_type=code' +
  '&client_id=4587994283757685' +
  '&redirect_uri=https%3A%2F%2Fgateway-api.salmonrock-4d3f2812.brazilsouth.azurecontainerapps.io%2Fintegrations%2Fmercado-livre%2Fcallback'

interface Marketplace {
  name: string
  displayName: string
  connectionType: 'OAuth' | 'API'
  oauthUrl?: string
  fields: {
    name: string
    label: string
    placeholder: string
    type?: string
    hint?: string
  }[]
}

function getMarketplaces(dict: Dictionary): Marketplace[] {
  return [
    {
      name: 'mercado-livre',
      displayName: 'Mercado Livre',
      connectionType: 'OAuth',
      oauthUrl: ML_OAUTH_URL,
      fields: [],
    },
    {
      name: 'shopee',
      displayName: 'Shopee',
      connectionType: 'API',
      fields: [
        { name: 'shop_id', ...dict.connectors.fields.shopId },
        { name: 'code', ...dict.connectors.fields.shopeeCode },
      ],
    },
    {
      name: 'amazon',
      displayName: 'Amazon',
      connectionType: 'API',
      fields: [
        { name: 'seller_id', ...dict.connectors.fields.sellerId },
        { name: 'refresh_token', ...dict.connectors.fields.refreshToken, type: 'password' },
      ],
    },
    {
      name: 'magalu',
      displayName: 'Magalu',
      connectionType: 'API',
      fields: [
        { name: 'client_id', ...dict.connectors.fields.clientId },
        { name: 'client_secret', ...dict.connectors.fields.clientSecret, type: 'password' },
      ],
    },
    {
      name: 'bling',
      displayName: 'Bling',
      connectionType: 'API',
      fields: [
        { name: 'api_key', ...dict.connectors.fields.blingApiKey, type: 'password' },
      ],
    },
    {
      name: 'olist',
      displayName: 'Olist',
      connectionType: 'API',
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

export function AddMarketplaceDialog({
  open,
  onOpenChange,
  existingConnectors = [],
  dict,
}: Props) {
  const [selected, setSelected] = useState<Marketplace | null>(null)
  const existing = new Set(existingConnectors.map(normalizeMarketplaceName))
  const available = getMarketplaces(dict).filter(
    (marketplace) => !existing.has(normalizeMarketplaceName(marketplace.name))
  )

  function handleOpenChange(nextOpen: boolean) {
    if (!nextOpen) setSelected(null)
    onOpenChange(nextOpen)
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="gap-0 overflow-hidden p-0 sm:max-w-2xl">
        <DialogHeader className="border-b border-border px-6 py-5 pr-14">
          {selected ? (
            <div className="flex items-center gap-3">
              <Button
                type="button"
                variant="ghost"
                size="icon-sm"
                onClick={() => setSelected(null)}
                aria-label={dict.connectors.addMarketplaceDialog.back}
              >
                <ArrowLeft className="size-4" />
              </Button>
              <MarketplaceLogo
                name={selected.name}
                displayName={selected.displayName}
                className="size-11"
              />
              <div className="min-w-0">
                <DialogTitle className="text-lg">
                  {formatMessage(dict.connectors.addMarketplaceDialog.connectTitle, {
                    displayName: selected.displayName,
                  })}
                </DialogTitle>
                <DialogDescription className="mt-1">
                  {selected.oauthUrl
                    ? dict.connectors.dialog.oauthDescription
                    : dict.connectors.addMarketplaceDialog.credentialDescription}
                </DialogDescription>
              </div>
            </div>
          ) : (
            <>
              <DialogTitle className="text-lg">
                {dict.connectors.addMarketplaceDialog.chooseTitle}
              </DialogTitle>
              <DialogDescription>
                {dict.connectors.addMarketplaceDialog.chooseDescription}
              </DialogDescription>
            </>
          )}
        </DialogHeader>

        {!selected ? (
          <div className="px-6 py-6">
            {available.length > 0 ? (
              <div className="grid gap-3 sm:grid-cols-2">
                {available.map((marketplace) => (
                  <button
                    key={marketplace.name}
                    type="button"
                    onClick={() => setSelected(marketplace)}
                    className="group flex min-h-20 items-center gap-3 rounded-lg border border-border bg-background p-3 text-left transition-[border-color,background-color,transform] duration-150 hover:-translate-y-0.5 hover:border-foreground/25 hover:bg-muted/35 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  >
                    <MarketplaceLogo
                      name={marketplace.name}
                      displayName={marketplace.displayName}
                      className="size-12"
                    />
                    <span className="min-w-0 flex-1">
                      <span className="block text-sm font-semibold">{marketplace.displayName}</span>
                      <span className="mt-1 flex items-center gap-1.5 text-xs text-muted-foreground">
                        <ShieldCheck className="size-3.5" />
                        {marketplace.connectionType}
                      </span>
                    </span>
                    <ArrowRight className="size-4 text-muted-foreground transition-transform group-hover:translate-x-0.5 group-hover:text-foreground" />
                  </button>
                ))}
              </div>
            ) : (
              <div className="flex min-h-48 flex-col items-center justify-center gap-3 text-center">
                <span className="flex size-11 items-center justify-center rounded-full border border-border bg-muted/40">
                  <Check className="size-5" />
                </span>
                <p className="max-w-sm text-sm text-muted-foreground">
                  {dict.connectors.addMarketplaceDialog.allConnected}
                </p>
              </div>
            )}
          </div>
        ) : selected.oauthUrl ? (
          <OAuthConnection
            marketplace={selected}
            dict={dict}
            onCancel={() => handleOpenChange(false)}
          />
        ) : (
          <CredentialConnection
            key={selected.name}
            marketplace={selected}
            dict={dict}
            onCancel={() => handleOpenChange(false)}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}

function OAuthConnection({
  marketplace,
  dict,
  onCancel,
}: {
  marketplace: Marketplace
  dict: Dictionary
  onCancel: () => void
}) {
  return (
    <div className="space-y-5 px-6 py-6">
      <div className="rounded-lg border border-border bg-muted/35 p-5">
        <div className="flex items-start gap-3">
          <ShieldCheck className="mt-0.5 size-5 shrink-0" />
          <div>
            <p className="text-sm font-medium">{dict.connectors.dialog.secureTitle}</p>
            <p className="mt-1 text-sm leading-6 text-muted-foreground">
              {dict.connectors.dialog.secureOauthDescription}
            </p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-[2rem_1fr] gap-x-3 gap-y-5">
        <FlowStep>1</FlowStep>
        <div>
          <p className="text-sm font-medium">
            {formatMessage(dict.connectors.dialog.step1Title, {
              displayName: marketplace.displayName,
            })}
          </p>
          <p className="mt-1 text-xs leading-5 text-muted-foreground">
            {dict.connectors.addMarketplaceDialog.oauthIntroPrefix}{' '}
            {dict.connectors.addMarketplaceDialog.oauthIntroHighlight}
            {formatMessage(dict.connectors.addMarketplaceDialog.oauthIntroSuffix, {
              displayName: marketplace.displayName,
            })}
          </p>
        </div>
        <FlowStep>2</FlowStep>
        <div>
          <p className="text-sm font-medium">{dict.connectors.dialog.finishConnection}</p>
          <p className="mt-1 text-xs leading-5 text-muted-foreground">
            {dict.connectors.addMarketplaceDialog.oauthNote}
          </p>
        </div>
      </div>

      <DialogFooter className="mx-0 mb-0 mt-1 px-0 pb-0">
        <Button type="button" variant="outline" onClick={onCancel}>
          {dict.connectors.dialog.cancel}
        </Button>
        <a href={marketplace.oauthUrl} className={buttonVariants({ size: 'lg' })}>
          {formatMessage(dict.connectors.addMarketplaceDialog.authorizeWith, {
            displayName: marketplace.displayName,
          })}
          <ExternalLink className="size-4" />
        </a>
      </DialogFooter>
    </div>
  )
}

function CredentialConnection({
  marketplace,
  dict,
  onCancel,
}: {
  marketplace: Marketplace
  dict: Dictionary
  onCancel: () => void
}) {
  const router = useRouter()
  const [state, formAction, isPending] = useActionState(authenticateAction, null)

  useEffect(() => {
    if (state?.success !== true) return
    const timer = setTimeout(() => {
      onCancel()
      router.refresh()
    }, 1200)
    return () => clearTimeout(timer)
  }, [state?.success, onCancel, router])

  return (
    <form action={formAction}>
      <div className="space-y-5 px-6 py-6">
        <input type="hidden" name="connectorName" value={marketplace.name} />

        {state?.success === false && (
          <div className="flex items-start gap-2 rounded-lg border border-destructive/30 bg-destructive/8 px-3 py-2.5 text-sm text-destructive">
            <AlertCircle className="mt-0.5 size-4 shrink-0" />
            <span>{state.error}</span>
          </div>
        )}
        {state?.success === true && (
          <div className="flex items-start gap-2 rounded-lg border border-border bg-muted/40 px-3 py-2.5 text-sm">
            <CheckCircle2 className="mt-0.5 size-4 shrink-0" />
            <span>
              {formatMessage(dict.connectors.dialog.success, {
                displayName: marketplace.displayName,
              })}
            </span>
          </div>
        )}

        <div className="grid gap-4 sm:grid-cols-2">
          {marketplace.fields.map((field) => (
            <div
              key={field.name}
              className={marketplace.fields.length === 1 ? 'space-y-1.5 sm:col-span-2' : 'space-y-1.5'}
            >
              <Label htmlFor={`add-${field.name}`}>{field.label}</Label>
              <Input
                id={`add-${field.name}`}
                name={field.name}
                type={field.type ?? 'text'}
                placeholder={field.placeholder}
                required
                disabled={isPending || state?.success === true}
                autoComplete="off"
                className="h-10"
              />
              {field.hint && (
                <p className="text-xs leading-5 text-muted-foreground">{field.hint}</p>
              )}
            </div>
          ))}
        </div>

        <div className="flex gap-3 rounded-lg border border-border bg-muted/35 p-3.5">
          <LockKeyhole className="mt-0.5 size-4 shrink-0" />
          <p className="text-xs leading-5 text-muted-foreground">
            {dict.connectors.dialog.secureCredentialsDescription}
          </p>
        </div>
      </div>

      <DialogFooter className="mx-0 mb-0 rounded-none px-6 py-4">
        <Button type="button" variant="outline" onClick={onCancel} disabled={isPending}>
          {dict.connectors.dialog.cancel}
        </Button>
        <Button type="submit" size="lg" disabled={isPending || state?.success === true}>
          {isPending ? (
            <>
              <Loader2 className="size-4 animate-spin" />
              {dict.connectors.dialog.connecting}
            </>
          ) : (
            <>
              <Plug className="size-4" />
              {dict.connectors.dialog.connect}
            </>
          )}
        </Button>
      </DialogFooter>
    </form>
  )
}

function FlowStep({ children }: { children: React.ReactNode }) {
  return (
    <span className="flex size-8 items-center justify-center rounded-full border border-border bg-background text-xs font-semibold">
      {children}
    </span>
  )
}
