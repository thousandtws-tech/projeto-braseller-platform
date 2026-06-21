import Link from 'next/link'

import { formatMessage } from '@/shared/i18n/format'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'

interface MarketingFooterProps {
  dict: Dictionary
  lang: Locale
}

export function MarketingFooter({ dict, lang }: MarketingFooterProps) {
  const nav = dict.marketing.nav
  const footer = dict.marketing.footer
  const year = new Date().getFullYear()

  const productLinks = [
    { href: 'inicio', label: nav.inicio },
    { href: 'planos', label: nav.planos },
  ]

  const companyLinks = [
    { href: 'sobre', label: nav.sobre },
    { href: 'contato', label: nav.contato },
  ]

  return (
    <footer className="border-t border-slate-200 bg-slate-50/60">
      <div className="mx-auto grid max-w-6xl grid-cols-1 gap-8 px-6 py-12 sm:grid-cols-2 lg:grid-cols-4">
        <div className="space-y-3">
          <Link href={`/${lang}/inicio`} className="flex items-center gap-3">
            <div className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-blue-600 shadow-sm">
              <span className="text-sm font-bold text-white">B</span>
            </div>
            <span className="text-sm font-semibold text-slate-900">Brasaller</span>
          </Link>
          <p className="max-w-xs text-sm text-slate-500">{footer.tagline}</p>
        </div>

        <FooterColumn title={footer.product} links={productLinks} lang={lang} />
        <FooterColumn title={footer.company} links={companyLinks} lang={lang} />

        <div className="space-y-3">
          <p className="text-sm font-semibold text-slate-900">{footer.legal}</p>
          <ul className="space-y-2 text-sm text-slate-500">
            <li>{footer.terms}</li>
            <li>{footer.privacy}</li>
          </ul>
        </div>
      </div>

      <div className="border-t border-slate-200 px-6 py-4">
        <p className="text-xs text-slate-400">
          {formatMessage(footer.rights, { year })}
        </p>
      </div>
    </footer>
  )
}

function FooterColumn({
  title,
  links,
  lang,
}: {
  title: string
  links: { href: string; label: string }[]
  lang: Locale
}) {
  return (
    <div className="space-y-3">
      <p className="text-sm font-semibold text-slate-900">{title}</p>
      <ul className="space-y-2 text-sm text-slate-500">
        {links.map(({ href, label }) => (
          <li key={href}>
            <Link href={`/${lang}/${href}`} className="transition-colors hover:text-slate-900">
              {label}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  )
}
