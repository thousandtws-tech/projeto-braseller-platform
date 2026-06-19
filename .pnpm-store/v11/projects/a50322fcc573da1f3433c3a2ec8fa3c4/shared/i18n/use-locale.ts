'use client'

import { usePathname } from 'next/navigation'
import { defaultLocale, isLocale, type Locale } from './config'

export function useLocale(): Locale {
  const pathname = usePathname()
  const segment = pathname.split('/')[1]
  return isLocale(segment) ? segment : defaultLocale
}
