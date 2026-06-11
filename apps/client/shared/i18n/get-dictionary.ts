import 'server-only'
import type { Locale } from './config'
import type { Dictionary } from './dictionaries/pt-BR'

const dictionaries: Record<Locale, () => Promise<Dictionary>> = {
  'pt-BR': () => import('./dictionaries/pt-BR').then((m) => m.default),
  en: () => import('./dictionaries/en').then((m) => m.default),
  es: () => import('./dictionaries/es').then((m) => m.default),
}

export async function getDictionary(locale: Locale): Promise<Dictionary> {
  const loader = dictionaries[locale] ?? dictionaries['pt-BR']
  return loader()
}

export type { Dictionary }
