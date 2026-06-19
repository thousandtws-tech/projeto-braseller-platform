import type { Metadata } from 'next'
import { notFound } from 'next/navigation'
import '../globals.css'
import { locales, isLocale } from '@/shared/i18n/config'

export const metadata: Metadata = {
  title: { default: 'Brasaller', template: '%s | Brasaller' },
  description: 'Plataforma de gestao financeira para vendedores de marketplace',
}

export function generateStaticParams() {
  return locales.map((lang) => ({ lang }))
}

interface Props {
  children: React.ReactNode
  params: Promise<{ lang: string }>
}

export default async function RootLayout({ children, params }: Props) {
  const { lang } = await params
  if (!isLocale(lang)) notFound()

  return (
    <html lang={lang} className="h-full antialiased">
      <body className="min-h-full flex flex-col bg-background font-sans text-foreground">
        {children}
      </body>
    </html>
  )
}
