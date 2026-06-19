export const locales = ['pt-BR', 'en', 'es'] as const

export type Locale = (typeof locales)[number]

export const defaultLocale: Locale = 'pt-BR'

export const LOCALE_COOKIE = 'NEXT_LOCALE'

export function isLocale(value: string | undefined | null): value is Locale {
  return !!value && (locales as readonly string[]).includes(value)
}
