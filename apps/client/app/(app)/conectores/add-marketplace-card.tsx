'use client'

import { useState } from 'react'
import { Plus } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { AddMarketplaceDialog } from './add-marketplace-dialog'

interface Props {
  existingConnectors: string[]
}

export function AddMarketplaceCard({ existingConnectors }: Props) {
  const [open, setOpen] = useState(false)

  return (
    <>
      <Card
        className="border-dashed cursor-pointer hover:border-primary/50 hover:bg-accent/30 transition-colors"
        onClick={() => setOpen(true)}
      >
        <CardContent className="flex flex-col items-center justify-center h-full min-h-[180px] gap-3 p-6">
          <div className="size-10 rounded-full border-2 border-dashed border-muted-foreground/30 flex items-center justify-center">
            <Plus className="size-5 text-muted-foreground/50" />
          </div>
          <div className="text-center">
            <p className="text-sm font-medium text-muted-foreground">Adicionar Marketplace</p>
            <p className="text-xs text-muted-foreground/70 mt-1">Shopee, Magalu, Bling...</p>
          </div>
        </CardContent>
      </Card>

      <AddMarketplaceDialog
        open={open}
        onOpenChange={setOpen}
        existingConnectors={existingConnectors}
      />
    </>
  )
}
