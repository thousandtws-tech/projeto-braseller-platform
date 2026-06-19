import type { Metadata } from 'next'
import { Geist } from 'next/font/google'
import { notFound } from 'next/navigation'

import '../globals.css'
import { locales, isLocale } from '@/shared/i18n/config'

const geist = Geist({
  subsets: ['latin'],
  variable: '--font-geist',
})

export const metadata: Metadata = {
  title: {
    default: 'Brasaller',
    template: '%s | Brasaller',
  },
  description:
    'Plataforma de gestão financeira para vendedores de marketplace',
}

export function generateStaticParams() {
  return locales.map((lang) => ({ lang }))
}

interface Props {
  children: React.ReactNode
  params: Promise<{ lang: string }>
}

export default async function RootLayout({
  children,
  params,
}: Props) {
  const { lang } = await params

  if (!isLocale(lang)) {
    notFound()
  }

  return (
    <html
      lang={lang}
      className={`${geist.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col bg-background text-foreground font-sans">
        {children}
      </body>
    </html>
  )
}