'use client'

import { useActionState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { Loader2, AlertCircle, CheckCircle2, Plug, ExternalLink } from 'lucide-react'
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
import type { Locale } from '@/shared/i18n/config'
import { mercadoLivreOAuthUrl } from '../lib/mercado-livre-oauth'

const LOCALE_MAP: Record<Locale, string> = { 'pt-BR': 'pt-BR', en: 'en-US', es: 'es-ES' }
const OAUTH_CONNECTORS = new Set(['mercado-livre', 'mercadolivre'])

interface CredentialField {
  name: string
  label: string
  placeholder: string
  type?: string
  hint?: string
}

function getCredentialFields(dict: Dictionary): Record<string, CredentialField[]> {
  return {
    shopee: [
      { name: 'shop_id', ...dict.connectors.fields.shopId },
      { name: 'code', ...dict.connectors.fields.shopeeCode },
    ],
    amazon: [
      { name: 'seller_id', ...dict.connectors.fields.sellerId },
      { name: 'refresh_token', ...dict.connectors.fields.refreshToken, type: 'password' },
    ],
  }
}

function getDefaultFields(dict: Dictionary): CredentialField[] {
  return [{ name: 'api_key', ...dict.connectors.fields.apiKey, type: 'password' }]
}

interface Props {
  connectorName: string
  displayName: string
  open: boolean
  onOpenChange: (open: boolean) => void
  dict: Dictionary
  lang: Locale
}

export function ConnectDialog({ connectorName, displayName, open, onOpenChange, dict, lang }: Props) {
  const router = useRouter()
  const [state, formAction, isPending] = useActionState(authenticateAction, null)
  const oauthUrl = OAUTH_CONNECTORS.has(connectorName) ? mercadoLivreOAuthUrl(lang) : null

  useEffect(() => {
    if (state?.success === true) {
      const t = setTimeout(() => {
        onOpenChange(false)
        router.refresh()
      }, 1500)
      return () => clearTimeout(t)
    }
  }, [state?.success, onOpenChange, router])

  const fields = getCredentialFields(dict)[connectorName] ?? getDefaultFields(dict)

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{formatMessage(dict.connectors.dialog.connectTitle, { displayName })}</DialogTitle>
          <DialogDescription>
            {oauthUrl
              ? dict.connectors.dialog.oauthDescription
              : dict.connectors.dialog.credentialDescription}
          </DialogDescription>
        </DialogHeader>

        {oauthUrl ? (
          <div className="space-y-4">
            <div className="rounded-lg border border-border bg-muted/40 p-4 text-sm text-muted-foreground">
              <p>
                {dict.connectors.addMarketplaceDialog.oauthIntroPrefix}{' '}
                <strong className="text-foreground">
                  {dict.connectors.addMarketplaceDialog.oauthIntroHighlight}
                </strong>
                {formatMessage(dict.connectors.addMarketplaceDialog.oauthIntroSuffix, { displayName })}
              </p>
              <p className="mt-2">{dict.connectors.addMarketplaceDialog.oauthNote}</p>
            </div>
            <DialogFooter className="border-0 bg-transparent p-0 mt-2">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                {dict.connectors.dialog.cancel}
              </Button>
              <a href={oauthUrl} className={buttonVariants()}>
                <ExternalLink className="size-4" />
                {formatMessage(dict.connectors.addMarketplaceDialog.authorizeWith, { displayName })}
              </a>
            </DialogFooter>
          </div>
        ) : (
          <form action={formAction} className="space-y-4">
            <input type="hidden" name="connectorName" value={connectorName} />

            {state?.success === false && (
              <div className="flex items-start gap-2 rounded-lg border border-destructive/30 bg-destructive/8 px-3 py-2.5 text-sm text-destructive">
                <AlertCircle className="size-4 mt-0.5 shrink-0" />
                <span>{state.error}</span>
              </div>
            )}
            {state?.success === true && (
              <div className="flex items-start gap-2 rounded-lg border border-emerald-500/30 bg-emerald-500/8 px-3 py-2.5 text-sm text-emerald-700 dark:text-emerald-400">
                <CheckCircle2 className="size-4 mt-0.5 shrink-0" />
                <span>
                  {formatMessage(dict.connectors.dialog.success, { displayName })}
                  {state.expires_at && (
                    <span className="text-muted-foreground ml-1">
                      {formatMessage(dict.connectors.dialog.expiresAt, {
                        date: new Date(state.expires_at).toLocaleDateString(LOCALE_MAP[lang]),
                      })}
                    </span>
                  )}
                </span>
              </div>
            )}

            {fields.map((field) => (
              <div key={field.name} className="space-y-1.5">
                <Label htmlFor={field.name}>{field.label}</Label>
                <Input
                  id={field.name}
                  name={field.name}
                  type={field.type ?? 'text'}
                  placeholder={field.placeholder}
                  required
                  disabled={isPending || state?.success === true}
                  autoComplete="off"
                />
                {field.hint && (
                  <p className="text-xs text-muted-foreground">{field.hint}</p>
                )}
              </div>
            ))}

            <DialogFooter className="border-0 bg-transparent p-0 mt-2">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={isPending}>
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
        )}
      </DialogContent>
    </Dialog>
  )
}
