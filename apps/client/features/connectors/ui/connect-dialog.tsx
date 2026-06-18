'use client'

import { useActionState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import {
  AlertCircle,
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
import type { Locale } from '@/shared/i18n/config'

import { MarketplaceLogo, normalizeMarketplaceName } from './marketplace-brand'

const LOCALE_MAP: Record<Locale, string> = { 'pt-BR': 'pt-BR', en: 'en-US', es: 'es-ES' }

const ML_OAUTH_URL =
  'https://auth.mercadolivre.com.br/authorization?response_type=code' +
  '&client_id=4587994283757685' +
  '&redirect_uri=https%3A%2F%2Fgateway-api.salmonrock-4d3f2812.brazilsouth.azurecontainerapps.io%2Fintegrations%2Fmercado-livre%2Fcallback'

const OAUTH_CONNECTORS: Record<string, { url: string }> = {
  'mercado-livre': { url: ML_OAUTH_URL },
}

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
    magalu: [
      { name: 'client_id', ...dict.connectors.fields.clientId },
      { name: 'client_secret', ...dict.connectors.fields.clientSecret, type: 'password' },
    ],
    bling: [
      { name: 'api_key', ...dict.connectors.fields.blingApiKey, type: 'password' },
    ],
    olist: [
      { name: 'api_key', ...dict.connectors.fields.olistApiKey, type: 'password' },
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

export function ConnectDialog({
  connectorName,
  displayName,
  open,
  onOpenChange,
  dict,
  lang,
}: Props) {
  const router = useRouter()
  const [state, formAction, isPending] = useActionState(authenticateAction, null)
  const normalizedName = normalizeMarketplaceName(connectorName)
  const oauth = OAUTH_CONNECTORS[normalizedName]
  const fields = getCredentialFields(dict)[normalizedName] ?? getDefaultFields(dict)

  useEffect(() => {
    if (state?.success !== true) return
    const timer = setTimeout(() => {
      onOpenChange(false)
      router.refresh()
    }, 1200)
    return () => clearTimeout(timer)
  }, [state?.success, onOpenChange, router])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="gap-0 overflow-hidden p-0 sm:max-w-xl">
        <DialogHeader className="border-b border-border px-6 py-5 pr-14">
          <div className="flex items-center gap-3">
            <MarketplaceLogo name={connectorName} displayName={displayName} className="size-12" />
            <div className="min-w-0">
              <DialogTitle className="text-lg">
                {formatMessage(dict.connectors.dialog.connectTitle, { displayName })}
              </DialogTitle>
              <DialogDescription className="mt-1">
                {oauth
                  ? dict.connectors.dialog.oauthDescription
                  : dict.connectors.dialog.credentialDescription}
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        {oauth ? (
          <div className="space-y-5 px-6 py-6">
            <div className="grid grid-cols-[2rem_1fr] gap-x-3 gap-y-5">
              <StepNumber complete>1</StepNumber>
              <div>
                <p className="font-medium">
                  {formatMessage(dict.connectors.dialog.step1Title, { displayName })}
                </p>
                <p className="mt-1 text-sm leading-5 text-muted-foreground">
                  {dict.connectors.addMarketplaceDialog.oauthNote}
                </p>
              </div>
              <StepNumber>2</StepNumber>
              <div>
                <p className="font-medium">{dict.connectors.dialog.finishConnection}</p>
                <p className="mt-1 text-sm leading-5 text-muted-foreground">
                  {dict.connectors.dialog.oauthReturnDescription}
                </p>
              </div>
            </div>

            <div className="flex gap-3 rounded-lg border border-border bg-muted/35 p-4">
              <ShieldCheck className="mt-0.5 size-5 shrink-0 text-foreground" />
              <div>
                <p className="text-sm font-medium">{dict.connectors.dialog.secureTitle}</p>
                <p className="mt-1 text-xs leading-5 text-muted-foreground">
                  {dict.connectors.dialog.secureOauthDescription}
                </p>
              </div>
            </div>

            <DialogFooter className="mx-0 mb-0 mt-1 px-0 pb-0">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                {dict.connectors.dialog.cancel}
              </Button>
              <a href={oauth.url} className={buttonVariants({ size: 'lg' })}>
                {formatMessage(dict.connectors.addMarketplaceDialog.authorizeWith, { displayName })}
                <ExternalLink className="size-4" />
              </a>
            </DialogFooter>
          </div>
        ) : (
          <form action={formAction}>
            <div className="space-y-5 px-6 py-6">
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <span className="flex size-5 items-center justify-center rounded-full bg-foreground text-background">
                  <Check className="size-3" />
                </span>
                <span>{dict.connectors.dialog.accountStep}</span>
                <ArrowRight className="size-3" />
                <span className="font-medium text-foreground">{dict.connectors.dialog.credentialsStep}</span>
              </div>

              <input type="hidden" name="connectorName" value={connectorName} />

              {state?.success === false && (
                <Feedback destructive icon={AlertCircle}>
                  {state.error}
                </Feedback>
              )}
              {state?.success === true && (
                <Feedback icon={CheckCircle2}>
                  {formatMessage(dict.connectors.dialog.success, { displayName })}
                  {state.expires_at && (
                    <span className="ml-1 text-muted-foreground">
                      {formatMessage(dict.connectors.dialog.expiresAt, {
                        date: new Date(state.expires_at).toLocaleDateString(LOCALE_MAP[lang]),
                      })}
                    </span>
                  )}
                </Feedback>
              )}

              <div className="space-y-4">
                {fields.map((field) => (
                  <div key={field.name} className="space-y-1.5">
                    <Label htmlFor={`connect-${field.name}`}>{field.label}</Label>
                    <Input
                      id={`connect-${field.name}`}
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
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={isPending}
              >
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
        )}
      </DialogContent>
    </Dialog>
  )
}

function StepNumber({ children, complete = false }: { children: React.ReactNode; complete?: boolean }) {
  return (
    <span
      className={
        complete
          ? 'flex size-8 items-center justify-center rounded-full bg-foreground text-xs font-semibold text-background'
          : 'flex size-8 items-center justify-center rounded-full border border-border bg-background text-xs font-semibold'
      }
    >
      {children}
    </span>
  )
}

function Feedback({
  children,
  destructive = false,
  icon: Icon,
}: {
  children: React.ReactNode
  destructive?: boolean
  icon: React.ComponentType<{ className?: string }>
}) {
  return (
    <div
      className={
        destructive
          ? 'flex items-start gap-2 rounded-lg border border-destructive/30 bg-destructive/8 px-3 py-2.5 text-sm text-destructive'
          : 'flex items-start gap-2 rounded-lg border border-border bg-muted/40 px-3 py-2.5 text-sm'
      }
    >
      <Icon className="mt-0.5 size-4 shrink-0" />
      <span>{children}</span>
    </div>
  )
}
