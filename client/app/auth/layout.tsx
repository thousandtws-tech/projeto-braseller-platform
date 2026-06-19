import type { Metadata } from 'next'
import '../globals.css'

export const metadata: Metadata = {
  title: { default: 'Brasaller', template: '%s | Brasaller' },
  description: 'Plataforma de gestao financeira para vendedores de marketplace',
}

export default function AuthRootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR" className="h-full antialiased">
      <body className="min-h-full flex flex-col bg-background font-sans text-foreground">
        {children}
      </body>
    </html>
  )
}
