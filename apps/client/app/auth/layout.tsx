import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import '../globals.css'
import { cn } from '@/shared/lib/utils'

const inter = Inter({ subsets: ['latin'], variable: '--font-sans' })

export const metadata: Metadata = {
  title: { default: 'Brasaller', template: '%s | Brasaller' },
  description: 'Plataforma de gestão financeira para vendedores de marketplace',
}

export default function AuthRootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR" className={cn('h-full antialiased', inter.variable)}>
      <body className="min-h-full flex flex-col font-sans bg-background text-foreground">
        {children}
      </body>
    </html>
  )
}
