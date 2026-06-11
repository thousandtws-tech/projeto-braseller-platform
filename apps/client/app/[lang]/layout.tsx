import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import { notFound } from 'next/navigation'
import '../globals.css'
import { cn } from '@/shared/lib/utils'
import { locales, isLocale } from '@/shared/i18n/config'

const inter = Inter({ subsets: ['latin'], variable: '--font-sans' })

export const metadata: Metadata = {
  title: { default: 'Brasaller', template: '%s | Brasaller' },
  description: 'Plataforma de gestão financeira para vendedores de marketplace',
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
    <html lang={lang} className={cn('h-full antialiased', inter.variable)}>
      <body className="min-h-full flex flex-col font-sans bg-background text-foreground">
        {children}
      </body>
    </html>
  )
}
