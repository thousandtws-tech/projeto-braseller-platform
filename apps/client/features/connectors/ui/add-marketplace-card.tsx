'use client'

import { useState } from 'react'
import { LockKeyhole, Plus } from 'lucide-react'
import { Card, CardContent } from '@/shared/ui/card'
import { ReadOnlyLock } from '@/shared/ui/read-only-lock'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'
import { AddMarketplaceDialog } from './add-marketplace-dialog'

interface Props {
  existingConnectors: string[]
  readOnly?: boolean
  dict: Dictionary
  lang: Locale
}

export function AddMarketplaceCard({ existingConnectors, readOnly = false, dict, lang }: Props) {
  const [open, setOpen] = useState(false)

  return (
    <>
      <Card
        className={`border-dashed transition-colors ${
          readOnly ? 'cursor-not-allowed opacity-75' : 'cursor-pointer hover:border-primary/50 hover:bg-accent/30'
        }`}
        onClick={() => {
          if (!readOnly) setOpen(true)
        }}
      >
        <CardContent className="flex flex-col items-center justify-center h-full min-h-[180px] gap-3 p-6">
          <div className="size-10 rounded-full border-2 border-dashed border-muted-foreground/30 flex items-center justify-center">
            {readOnly
              ? <LockKeyhole className="size-5 animate-pulse text-amber-600" />
              : <Plus className="size-5 text-muted-foreground/50" />
            }
          </div>
          <div className="text-center">
            <p className="text-sm font-medium text-muted-foreground">{dict.connectors.addMarketplaceCard.title}</p>
            <p className="text-xs text-muted-foreground/70 mt-1">{dict.connectors.addMarketplaceCard.subtitle}</p>
          </div>
          {readOnly && <ReadOnlyLock compact />}
        </CardContent>
      </Card>

      <AddMarketplaceDialog
        open={open}
        onOpenChange={setOpen}
        existingConnectors={existingConnectors}
        dict={dict}
        lang={lang}
      />
    </>
  )
}
