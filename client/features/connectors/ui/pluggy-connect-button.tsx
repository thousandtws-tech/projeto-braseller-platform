'use client'

import dynamic from 'next/dynamic'
import { useState, useTransition } from 'react'
import { AlertCircle, CheckCircle2, Loader2 } from 'lucide-react'

import { createPluggyConnectTokenAction } from '@/features/connectors/server/actions'
import { appToast } from '@/shared/lib/toast'
import { Button } from '@/shared/ui/button'

const PluggyConnect = dynamic(
  () => import('react-pluggy-connect').then((mod) => mod.PluggyConnect),
  { ssr: false }
)

type PluggyItemData = {
  item?: { id?: string }
  itemId?: string
}

export function PluggyConnectButton({ onCancel }: { onCancel: () => void }) {
  const [connectToken, setConnectToken] = useState('')
  const [includeSandbox, setIncludeSandbox] = useState(true)
  const [connectedItemId, setConnectedItemId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isPending, startTransition] = useTransition()

  function handleStart() {
    setError(null)
    setConnectedItemId(null)
    startTransition(async () => {
      const result = await createPluggyConnectTokenAction()
      if (!result.success) {
        setError(result.error)
        appToast.error(result.error)
        return
      }

      setIncludeSandbox(result.includeSandbox)
      setConnectToken(result.accessToken)
    })
  }

  return (
    <div className="flex flex-col gap-4 px-6 py-6">
      <div className="rounded-md border border-border bg-muted/35 p-4 text-sm text-muted-foreground">
        O fluxo do Open Finance é aberto com segurança pelo Pluggy Connect. As credenciais da Pluggy ficam somente no backend e nunca são enviadas ao navegador.
      </div>

      {error ? (
        <div className="flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>{error}</span>
        </div>
      ) : null}

      {connectedItemId ? (
        <div className="flex items-start gap-2 rounded-md border border-emerald-500/25 bg-emerald-500/10 p-3 text-sm text-emerald-700 dark:text-emerald-300">
          <CheckCircle2 className="mt-0.5 size-4 shrink-0" />
          <span>Conta conectada com sucesso. Item Pluggy: {connectedItemId}</span>
        </div>
      ) : null}

      {connectToken ? (
        <PluggyConnect
          connectToken={connectToken}
          includeSandbox={includeSandbox}
          onSuccess={(itemData: PluggyItemData) => {
            const itemId = itemData.item?.id ?? itemData.itemId ?? 'conectado'
            setConnectedItemId(itemId)
            appToast.success('Open Finance conectado com sucesso.')
          }}
          onError={(pluggyError: unknown) => {
            const message = pluggyError instanceof Error ? pluggyError.message : 'Falha ao conectar com o Open Finance.'
            setError(message)
            appToast.error(message)
          }}
        />
      ) : null}

      <div className="flex items-center justify-end gap-2 border-t border-border pt-4">
        <Button type="button" variant="outline" onClick={onCancel}>Cancelar</Button>
        <Button type="button" onClick={handleStart} disabled={isPending}>
          {isPending ? <Loader2 className="size-4 animate-spin" /> : null}
          {connectToken ? 'Gerar novo token' : 'Abrir Pluggy Connect'}
        </Button>
      </div>
    </div>
  )
}
