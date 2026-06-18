import { CheckCircle2 } from 'lucide-react'

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <main className="grid min-h-dvh bg-background lg:grid-cols-[minmax(420px,0.86fr)_1.14fr]">
      <section className="relative hidden overflow-hidden border-r border-border bg-foreground text-background lg:flex lg:flex-col lg:justify-between lg:p-12 xl:p-16">
        <div className="flex items-center gap-3">
          <div className="flex size-9 items-center justify-center rounded-md bg-background text-sm font-bold text-foreground">
            B
          </div>
          <span className="text-xl font-semibold tracking-[-0.035em]">Brasaller</span>
        </div>

        <div className="max-w-lg">
          <p className="mb-6 text-xs font-medium uppercase tracking-[0.18em] text-background/55">
            Operação financeira conectada
          </p>
          <h1 className="max-w-md text-4xl font-semibold leading-[1.08] tracking-[-0.05em] xl:text-5xl">
            Clareza para decidir. Automação para avançar.
          </h1>
          <p className="mt-6 max-w-md text-base leading-7 text-background/62">
            Vendas, despesas, estoque e contabilidade reunidos em um fluxo simples e verificável.
          </p>

          <div className="mt-10 flex flex-col gap-4 border-t border-background/15 pt-7">
            {[
              'Conciliação automática de marketplaces',
              'Visão financeira em tempo real',
              'DRE, balanço e contador no mesmo lugar',
            ].map((item) => (
              <div key={item} className="flex items-center gap-3 text-sm text-background/78">
                <CheckCircle2 className="size-4" />
                {item}
              </div>
            ))}
          </div>
        </div>

        <div className="flex items-center justify-between border-t border-background/15 pt-6 text-xs text-background/45">
          <span>© 2026 Brasaller</span>
          <span>Segurança e privacidade por padrão</span>
        </div>
      </section>

      <section className="flex min-h-dvh items-center justify-center px-5 py-10 sm:px-8 lg:px-12">
        <div className="w-full max-w-[440px]">{children}</div>
      </section>
    </main>
  )
}
