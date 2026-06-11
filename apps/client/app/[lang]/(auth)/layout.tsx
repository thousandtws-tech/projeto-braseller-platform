export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex">
      {/* Left panel - brand */}
      <div className="hidden lg:flex lg:w-1/2 bg-sidebar flex-col justify-between p-12">
        <div className="flex items-center gap-3">
          <div className="size-9 rounded-xl bg-sidebar-primary flex items-center justify-center">
            <span className="text-sidebar-primary-foreground font-bold text-lg">B</span>
          </div>
          <span className="text-sidebar-foreground font-semibold text-xl">Brasaller</span>
        </div>

        <div className="space-y-4">
          <blockquote className="text-sidebar-foreground/80 text-lg leading-relaxed">
            &ldquo;Conecte seus marketplaces, consolide suas vendas e tenha controle total
            das suas finanças em um só lugar.&rdquo;
          </blockquote>
          <div className="flex items-center gap-3 mt-6">
            <div className="size-10 rounded-full bg-sidebar-accent flex items-center justify-center">
              <span className="text-sidebar-accent-foreground font-semibold text-sm">BR</span>
            </div>
            <div>
              <p className="text-sidebar-foreground font-medium text-sm">Equipe Brasaller</p>
              <p className="text-sidebar-foreground/50 text-xs">Brasaller</p>
            </div>
          </div>
        </div>

        <div className="flex gap-6 text-sidebar-foreground/40 text-xs">
          <span>© 2026 Brasaller</span>
          <span>Clarituz Development</span>
        </div>
      </div>

      {/* Right panel - form */}
      <div className="flex-1 flex items-center justify-center p-8 bg-background">
        <div className="w-full max-w-sm">{children}</div>
      </div>
    </div>
  )
}
