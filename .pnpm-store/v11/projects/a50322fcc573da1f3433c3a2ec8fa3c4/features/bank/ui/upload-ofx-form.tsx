'use client'

import { useActionState, useRef } from 'react'
import { Upload, Loader2, CheckCircle2, AlertCircle } from 'lucide-react'
import { importOfxAction } from '@/features/bank/server/actions'
import { ReadOnlyLock } from '@/shared/ui/read-only-lock'
import { formatMessage } from '@/shared/i18n/format'
import type { Dictionary } from '@/shared/i18n/get-dictionary'

interface Props {
  readOnly?: boolean
  dict: Dictionary
}

export function UploadOfxForm({ readOnly = false, dict }: Props) {
  const [state, action, isPending] = useActionState(importOfxAction, null)
  const inputRef = useRef<HTMLInputElement>(null)
  const disabled = readOnly || isPending

  return (
    <form action={action} className="flex flex-col gap-3">
      {readOnly && <ReadOnlyLock />}
      <label
        className={`flex min-h-40 flex-col items-center justify-center gap-3 rounded-lg border border-dashed border-border bg-muted/25 px-6 py-8 text-center transition-colors ${
          readOnly ? 'cursor-not-allowed opacity-70' : 'cursor-pointer hover:border-foreground/35 hover:bg-muted/50'
        }`}
        onClick={() => {
          if (!readOnly) inputRef.current?.click()
        }}
      >
        <div className="flex size-11 items-center justify-center rounded-full border border-border bg-background"><Upload className={`size-5 text-muted-foreground ${readOnly ? 'animate-pulse' : ''}`} /></div>
        <span className="text-sm font-medium">{dict.bank.importOfx.dropzone}</span>
        <span className="text-xs text-muted-foreground">{dict.bank.importOfx.format}</span>
      </label>
      <input
        ref={inputRef}
        name="file"
        type="file"
        accept=".ofx,.qfx,text/plain"
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
          <span>{formatMessage(dict.bank.importOfx.success, { count: state.count })}</span>
        </div>
      )}
      {isPending && (
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Loader2 className="size-4 animate-spin" />
          {dict.bank.importOfx.importing}
        </div>
      )}
    </form>
  )
}
