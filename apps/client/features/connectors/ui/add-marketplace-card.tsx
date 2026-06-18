'use client'

import { useState } from 'react'
import { LockKeyhole, Plus } from 'lucide-react'
import { Card, CardContent } from '@/shared/ui/card'
import { ReadOnlyLock } from '@/shared/ui/read-only-lock'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import { AddMarketplaceDialog } from './add-marketplace-dialog'

interface Props {
  existingConnectors: string[]
  readOnly?: boolean
  dict: Dictionary
}

export function AddMarketplaceCard({ existingConnectors, readOnly = false, dict }: Props) {
  const [open, setOpen] = useState(false)

  return (
    <>
      <Card
        className={`border-dashed transition-colors ${
          readOnly ? 'cursor-not-allowed opacity-75' : 'cursor-pointer hover:border-foreground/35 hover:bg-muted/35'
        }`}
        onClick={() => {
          if (!readOnly) setOpen(true)
        }}
      >
        <CardContent className="flex h-full min-h-[210px] flex-col items-center justify-center gap-3 p-6">
          <div className="flex size-10 items-center justify-center rounded-full border border-dashed border-muted-foreground/40">
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
      />
    </>
  )
}
