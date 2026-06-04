import Link from "next/link"

export function Footer() {
  return (
    <footer className="border-t border-border bg-card py-4 px-6">
      <div className="flex flex-col md:flex-row items-center justify-between gap-4 text-sm text-muted-foreground">
        <p>&copy; {new Date().getFullYear()} SaaS Platform. Todos os direitos reservados.</p>
        <nav className="flex items-center gap-4">
          <Link href="/termos" className="hover:text-foreground transition-colors">
            Termos de Uso
          </Link>
          <Link href="/privacidade" className="hover:text-foreground transition-colors">
            Privacidade
          </Link>
          <Link href="/suporte" className="hover:text-foreground transition-colors">
            Suporte
          </Link>
        </nav>
      </div>
    </footer>
  )
}
