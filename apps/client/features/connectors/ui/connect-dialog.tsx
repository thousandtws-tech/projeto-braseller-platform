'use client'

import { useActionState, useState, useEffect } from 'react'
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

const ML_OAUTH_URL =
  'https://auth.mercadolivre.com.br/authorization?response_type=code' +
  '&client_id=4587994283757685' +
  '&redirect_uri=https%3A%2F%2Fgateway-api.salmonrock-4d3f2812.brazilsouth.azurecontainerapps.io%2Fintegrations%2Fmercado-livre%2Fcallback'

// Nota: o gateway callback troca o code pelos tokens e redireciona
// para /conectores/callback?code=... no nosso frontend

// Conectores que usam OAuth redirect em vez de formulário de credenciais
const OAUTH_CONNECTORS: Record<string, { url: string; label: string }> = {
  'mercado-livre': { url: ML_OAUTH_URL, label: 'Autorizar com Mercado Livre' },
  mercadolivre:   { url: ML_OAUTH_URL, label: 'Autorizar com Mercado Livre' },
}

// Campos de credencial para conectores não-OAuth
const CREDENTIAL_FIELDS: Record<string, { name: string; label: string; placeholder: string; type?: string; hint?: string }[]> = {
  shopee: [
    { name: 'shop_id', label: 'Shop ID', placeholder: 'ID da sua loja no Seller Center', hint: 'Encontrado em Minha conta > Loja no Shopee Seller Center.' },
    { name: 'code',    label: 'Código de autorização', placeholder: 'Código gerado pelo Shopee OAuth', hint: 'Gere em Shopee Partner Center > Autorizar aplicativo, copie o código retornado.' },
  ],
  amazon: [
    { name: 'seller_id',     label: 'Seller ID',     placeholder: 'ID do vendedor (Seller Central)', hint: 'Encontrado em Seller Central > Configurações de conta.' },
    { name: 'refresh_token', label: 'Refresh Token', placeholder: 'Token gerado no Seller Central', type: 'password', hint: 'Gere em Seller Central > Aplicativos e serviços > Meus aplicativos.' },
  ],
}

const DEFAULT_FIELDS = [
  { name: 'api_key', label: 'API Key', placeholder: 'Chave de acesso', type: 'password' },
]

interface Props {
  connectorName: string
  displayName: string
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function ConnectDialog({ connectorName, displayName, open, onOpenChange }: Props) {
  const router = useRouter()
  const [state, formAction, isPending] = useActionState(authenticateAction, null)
  const [oauthOpened, setOauthOpened] = useState(false)
  const oauth = OAUTH_CONNECTORS[connectorName]

  // Fecha o modal e revalida os dados do servidor após conexão bem-sucedida
  useEffect(() => {
    if (state?.success === true) {
      const t = setTimeout(() => {
        onOpenChange(false)
        setOauthOpened(false)
        router.refresh()
      }, 1500)
      return () => clearTimeout(t)
    }
  }, [state?.success, onOpenChange, router])
  const fields = CREDENTIAL_FIELDS[connectorName] ?? DEFAULT_FIELDS

  return (
    <Dialog open={open} onOpenChange={(o) => { if (!o) setOauthOpened(false); onOpenChange(o) }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Conectar {displayName}</DialogTitle>
          <DialogDescription>
            {oauth
              ? 'Autorize o acesso e cole o código gerado para finalizar a conexão.'
              : 'Informe as credenciais da sua conta para autorizar a integração.'}
          </DialogDescription>
        </DialogHeader>

        {/* OAuth flow — 2 etapas */}
        {oauth ? (
          <form action={formAction} className="space-y-4">
            <input type="hidden" name="connectorName" value={connectorName} />

            {/* Etapa 1 — Abrir autorização */}
            <div className="rounded-lg border border-border bg-muted/40 p-4 space-y-3">
              <div className="flex items-start gap-2.5">
                <span className="flex size-5 shrink-0 items-center justify-center rounded-full bg-primary text-[10px] font-bold text-primary-foreground mt-0.5">1</span>
                <div className="space-y-2 flex-1">
                  <p className="text-sm font-medium">Autorizar no {displayName}</p>
                  <p className="text-xs text-muted-foreground">
                    Clique para abrir a página de autorização em uma nova aba. Após aprovar, copie o código exibido na URL.
                  </p>
                  <a
                    href={oauth.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className={buttonVariants({ variant: 'outline', size: 'sm' })}
                    onClick={() => setOauthOpened(true)}
                  >
                    <ExternalLink className="size-3.5" />
                    Abrir autorização {displayName}
                  </a>
                </div>
              </div>
            </div>

            {/* Etapa 2 — Colar o code */}
            <div className={`rounded-lg border p-4 space-y-3 transition-opacity ${oauthOpened ? 'border-border bg-muted/40 opacity-100' : 'border-border/40 bg-muted/20 opacity-50 pointer-events-none'}`}>
              <div className="flex items-start gap-2.5">
                <span className="flex size-5 shrink-0 items-center justify-center rounded-full bg-primary text-[10px] font-bold text-primary-foreground mt-0.5">2</span>
                <div className="space-y-2 flex-1">
                  <p className="text-sm font-medium">Cole o código de autorização</p>
                  <p className="text-xs text-muted-foreground">
                    Após autorizar, o código estará na URL de retorno. Cole-o abaixo.
                  </p>
                  <input
                    name="code"
                    type="text"
                    placeholder="TG-xxxxxxxxxxxxxxxxxxxxxxxx"
                    required={oauthOpened}
                    disabled={isPending || !oauthOpened}
                    autoComplete="off"
                    className="flex h-8 w-full rounded-lg border border-input bg-transparent px-3 text-sm font-mono placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50"
                  />
                  <input
                    type="hidden"
                    name="redirect_uri"
                    value="https://gateway-api.salmonrock-4d3f2812.brazilsouth.azurecontainerapps.io/integrations/mercado-livre/callback"
                  />
                </div>
              </div>
            </div>

            {state?.success === false && (
              <div className="flex items-start gap-2 rounded-lg border border-destructive/30 bg-destructive/8 px-3 py-2.5 text-sm text-destructive">
                <AlertCircle className="size-4 mt-0.5 shrink-0" />
                <span>{state.error}</span>
              </div>
            )}
            {state?.success === true && (
              <div className="flex items-start gap-2 rounded-lg border border-emerald-500/30 bg-emerald-500/8 px-3 py-2.5 text-sm text-emerald-700 dark:text-emerald-400">
                <CheckCircle2 className="size-4 mt-0.5 shrink-0" />
                <span>{displayName} conectado com sucesso!</span>
              </div>
            )}

            <DialogFooter className="border-0 bg-transparent p-0 mt-2">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={isPending}>
                Cancelar
              </Button>
              <Button type="submit" disabled={isPending || !oauthOpened || state?.success === true}>
                {isPending
                  ? <><Loader2 className="size-4 animate-spin" />Conectando...</>
                  : <><Plug className="size-4" />Finalizar conexão</>
                }
              </Button>
            </DialogFooter>
          </form>
        ) : (
          /* Credential form */
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
                  {displayName} conectado com sucesso!
                  {state.expires_at && (
                    <span className="text-muted-foreground ml-1">
                      Expira em {new Date(state.expires_at).toLocaleDateString('pt-BR')}
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
        )}
      </DialogContent>
    </Dialog>
  )
}
