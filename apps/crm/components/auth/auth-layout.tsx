interface AuthLayoutProps {
  children: React.ReactNode
}

export function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <div className="min-h-screen flex">
      {/* Lado esquerdo - Branding */}
      <div className="hidden lg:flex lg:w-1/2 bg-muted relative overflow-hidden border-r border-border">
        <div className="relative z-10 flex flex-col justify-between p-12 text-foreground">
          <div>
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-border flex items-center justify-center">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  className="w-6 h-6"
                >
                  <path d="m8 3 4 8 5-5 5 15H2L8 3z" />
                </svg>
              </div>
              <span className="text-2xl font-bold">Brasaller</span>
            </div>
          </div>
          
          <div className="space-y-6">
            <blockquote className="text-xl font-medium leading-relaxed">
              &ldquo;A plataforma transformou a gestão das minhas vendas no 
              Mercado Livre. Agora tenho controle total do meu financeiro.&rdquo;
            </blockquote>
            <div>
              <p className="font-semibold">Carlos Vendedor</p>
              <p className="text-muted-foreground text-sm">Vendedor Gold, Mercado Livre</p>
            </div>
          </div>

          <div className="flex gap-8 text-sm">
            <div>
              <p className="text-3xl font-bold">5k+</p>
              <p className="text-muted-foreground">Vendedores</p>
            </div>
            <div>
              <p className="text-3xl font-bold">R$ 2M+</p>
              <p className="text-muted-foreground">Processados/mês</p>
            </div>
            <div>
              <p className="text-3xl font-bold">14</p>
              <p className="text-muted-foreground">Dias grátis</p>
            </div>
          </div>
        </div>
        
        {/* Elementos decorativos */}
        <div className="absolute -bottom-32 -right-32 w-96 h-96 rounded-full border-2 border-dashed border-border" />
        <div className="absolute -top-16 -right-16 w-64 h-64 rounded-full border-2 border-dashed border-border" />
      </div>

      {/* Lado direito - Formulário */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-6 sm:p-12">
        <div className="w-full max-w-md">
          {/* Logo mobile */}
          <div className="lg:hidden flex items-center justify-center gap-3 mb-8">
            <div className="w-10 h-10 rounded-lg bg-muted border border-border flex items-center justify-center">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                className="w-6 h-6 text-foreground"
              >
                <path d="m8 3 4 8 5-5 5 15H2L8 3z" />
              </svg>
            </div>
            <span className="text-2xl font-bold text-foreground">Brasaller</span>
          </div>
          {children}
        </div>
      </div>
    </div>
  )
}
