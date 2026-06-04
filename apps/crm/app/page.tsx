import Link from "next/link"
import { Button } from "@/components/ui/button"

export default function HomePage() {
  return (
    <main className="min-h-screen flex flex-col items-center justify-center bg-background px-6">
      <div className="flex flex-col items-center gap-8 text-center max-w-2xl">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-xl bg-muted border border-border flex items-center justify-center">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              className="w-7 h-7 text-foreground"
            >
              <path d="m8 3 4 8 5-5 5 15H2L8 3z" />
            </svg>
          </div>
          <span className="text-3xl font-bold text-foreground">Brasaller</span>
        </div>

        <div className="space-y-4">
          <h1 className="text-4xl sm:text-5xl font-bold tracking-tight text-foreground text-balance">
            Gestão financeira para vendedores de marketplace
          </h1>
          <p className="text-lg text-muted-foreground text-pretty leading-relaxed">
            Conecte seus marketplaces, consolide vendas, gere relatórios financeiros
            e DRE automaticamente. Ideal para vendedores e contadores.
          </p>
        </div>

        <div className="flex flex-col sm:flex-row gap-4">
          <Button asChild size="lg" className="h-12 px-8 text-base">
            <Link href="/cadastro">
              Começar trial de 14 dias
            </Link>
          </Button>
          <Button asChild variant="outline" size="lg" className="h-12 px-8 text-base">
            <Link href="/login">
              Já tenho uma conta
            </Link>
          </Button>
        </div>

        <p className="text-sm text-muted-foreground">
          Conecte Mercado Livre, Shopee, Amazon e mais
        </p>
      </div>
    </main>
  )
}
