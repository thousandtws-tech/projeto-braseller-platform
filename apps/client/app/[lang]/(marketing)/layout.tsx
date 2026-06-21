import { notFound } from 'next/navigation'
import { MarketingHeader, MarketingFooter } from '@/widgets/marketing-shell'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import { isLocale } from '@/shared/i18n/config'

interface Props {
  children: React.ReactNode
  params: Promise<{ lang: string }>
}

export default async function MarketingLayout({ children, params }: Props) {
  const { lang } = await params
  if (!isLocale(lang)) notFound()

  const dict = await getDictionary(lang)

  return (
    <div className="flex min-h-screen flex-col">
      <MarketingHeader dict={dict} lang={lang} />
      <main className="flex-1">{children}</main>
      <MarketingFooter dict={dict} lang={lang} />
    </div>
  )
}
