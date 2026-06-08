'use client'

import { useActionState, useRef } from 'react'
import { Upload, Loader2, CheckCircle2, AlertCircle } from 'lucide-react'
import { importNfeXmlAction } from '@/features/stock/server/actions'
import { ReadOnlyLock } from '@/shared/ui/read-only-lock'

export function UploadNfeForm({ readOnly = false }: { readOnly?: boolean }) {
  const [state, action, isPending] = useActionState(importNfeXmlAction, null)
  const inputRef = useRef<HTMLInputElement>(null)
  const disabled = readOnly || isPending

  return (
    <form action={action} className="space-y-3">
      {readOnly && <ReadOnlyLock />}
      <label
        className={`flex flex-col items-center gap-2 rounded-xl border-2 border-dashed border-border bg-muted/30 px-6 py-8 transition-colors ${
          readOnly ? 'cursor-not-allowed opacity-70' : 'cursor-pointer hover:border-primary/40 hover:bg-muted/50'
        }`}
        onClick={() => {
          if (!readOnly) inputRef.current?.click()
        }}
      >
        <Upload className={`size-8 text-muted-foreground ${readOnly ? 'animate-pulse' : ''}`} />
        <span className="text-sm font-medium">Arraste o XML da NF-e ou clique para selecionar</span>
        <span className="text-xs text-muted-foreground">Formato: .xml — NF-e do fornecedor</span>
      </label>
      <input
        ref={inputRef}
        name="file"
        type="file"
        accept=".xml,text/xml,application/xml"
        className="hidden"
        onChange={(e) => e.target.form?.requestSubmit()}
        disabled={disabled}
      />

      {state?.success === false && (
        <div className="flex items-start gap-2 rounded-lg border border-destructive/30 bg-destructive/8 px-3 py-2.5 text-sm text-destructive">
          <AlertCircle className="size-4 mt-0.5 shrink-0" />
          <span>{state.error}</span>
        </div>
      )}
      {state?.success === true && (
        <div className="flex items-center gap-2 rounded-lg border border-emerald-500/30 bg-emerald-500/8 px-3 py-2.5 text-sm text-emerald-700 dark:text-emerald-400">
          <CheckCircle2 className="size-4 mt-0.5 shrink-0" />
          <span>NF-e importada com sucesso! Estoque e CMV atualizados.</span>
        </div>
      )}
      {isPending && (
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Loader2 className="size-4 animate-spin" />
          Importando XML...
        </div>
      )}
    </form>
  )
}
