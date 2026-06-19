import 'server-only'
import { cookies } from 'next/headers'
import { defaultLocale, isLocale, LOCALE_COOKIE, type Locale } from './config'

export async function getLocale(): Promise<Locale> {
  const store = await cookies()
  const value = store.get(LOCALE_COOKIE)?.value
  return isLocale(value) ? value : defaultLocale
}

export async function localePath(path: string): Promise<string> {
  const locale = await getLocale()
  return `/${locale}${path.startsWith('/') ? path : `/${path}`}`
}
