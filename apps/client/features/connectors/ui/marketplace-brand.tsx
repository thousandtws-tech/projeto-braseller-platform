import Image from 'next/image'
import { FlaskConical, Store } from 'lucide-react'

import { cn } from '@/shared/lib/utils'

interface MarketplaceBrand {
  displayName: string
  logoSrc: string
}

const MARKETPLACE_BRANDS: Record<string, MarketplaceBrand> = {
  'mercado-livre': { displayName: 'Mercado Livre', logoSrc: '/favicons/180x180.png' },
  mercadolivre: { displayName: 'Mercado Livre', logoSrc: '/favicons/180x180.png' },
  mercadolibre: { displayName: 'Mercado Livre', logoSrc: '/favicons/180x180.png' },
  shopee: { displayName: 'Shopee', logoSrc: '/favicons/favicon.ico' },
  amazon: { displayName: 'Amazon', logoSrc: '/favicons/amazon.ico' },
  magalu: { displayName: 'Magalu', logoSrc: '/favicons/magalu.ico' },
  bling: { displayName: 'Bling', logoSrc: '/favicons/bling.ico' },
  olist: { displayName: 'Olist', logoSrc: '/favicons/olist.ico' },
}

export function normalizeMarketplaceName(name: string) {
  const normalized = name.toLowerCase().trim()
  if (normalized === 'mercadolivre' || normalized === 'mercadolibre') {
    return 'mercado-livre'
  }
  return normalized
}

interface MarketplaceLogoProps {
  name: string
  displayName?: string
  className?: string
  imageClassName?: string
}

export function MarketplaceLogo({
  name,
  displayName,
  className,
  imageClassName,
}: MarketplaceLogoProps) {
  const brand = MARKETPLACE_BRANDS[name.toLowerCase().trim()]
  const isTestConnector = name.toLowerCase().trim() === 'sandbox'

  return (
    <span
      className={cn(
        'flex size-11 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-border bg-white shadow-[0_1px_2px_rgba(0,0,0,0.04)]',
        className
      )}
    >
      {brand ? (
        <Image
          src={brand.logoSrc}
          alt={`${displayName ?? brand.displayName} logo`}
          width={44}
          height={44}
          unoptimized
          className={cn('size-full object-contain p-1.5', imageClassName)}
        />
      ) : isTestConnector ? (
        <FlaskConical className="size-5 text-violet-600" aria-hidden="true" />
      ) : (
        <Store className="size-5 text-muted-foreground" aria-hidden="true" />
      )}
    </span>
  )
}
