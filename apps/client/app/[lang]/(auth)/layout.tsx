import Image from "next/image";
import Banner from "@/public/images/ecad2e66-d016-43b5-8427-bc520da3992b.png";

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen">
      <div className="relative hidden overflow-hidden bg-sidebar p-12 lg:flex lg:w-1/2 lg:flex-col lg:justify-between">
        <div className="absolute inset-0 overflow-hidden">
          <div className="absolute -right-32 -top-32 h-96 w-96 animate-glow rounded-full bg-blue-500/10 blur-3xl" />

          <div
            className="absolute -bottom-32 -left-32 h-96 w-96 animate-glow rounded-full bg-indigo-500/10 blur-3xl"
            style={{ animationDelay: "3s" }}
          />

          <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(59,130,246,0.12),transparent_35%),radial-gradient(circle_at_bottom_left,rgba(99,102,241,0.10),transparent_40%)]" />
        </div>

        <div className="relative z-10 flex flex-1 items-center justify-center py-10">
          <div className="animate-float relative w-full max-w-2xl">
            <div className="animate-glow absolute -inset-4 rounded-[2rem] bg-blue-500/10 blur-2xl" />

            <div className="relative overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/5 p-2 shadow-2xl shadow-black/30 backdrop-blur">
              <Image
                src={Banner}
                alt="Ilustração de dashboard SaaS Brasaller"
                width={1600}
                height={900}
                priority
                className="h-auto w-full rounded-[1.35rem] object-cover transition-all duration-700 hover:scale-[1.02]"
              />

              <div className="absolute inset-0 rounded-[1.35rem] bg-gradient-to-tr from-black/40 via-transparent to-primary/10" />
            </div>

            <div className="animate-float-card absolute -bottom-6 left-8 rounded-2xl border border-emerald-500/20 bg-black/40 px-4 py-3 shadow-xl shadow-emerald-500/10 backdrop-blur-xl">
              <p className="text-xs font-medium text-emerald-300">
                Receita consolidada
              </p>

              <p className="mt-1 text-lg font-bold text-white">
                R$ 128.450,00
              </p>
            </div>

            <div
              className="animate-float-card absolute -right-6 top-8 overflow-hidden rounded-2xl border border-primary/20 bg-black/40 px-5 py-4 shadow-2xl shadow-primary/10 backdrop-blur-xl"
              style={{ animationDelay: "1.5s" }}
            >
              <div className="absolute inset-0 bg-gradient-to-br from-primary/20 via-transparent to-transparent" />

              <div className="relative">
                <p className="text-xs font-medium uppercase tracking-wide text-white/70">
                  Marketplaces ativos
                </p>

                <p className="mt-1 text-2xl font-bold text-white">8 canais</p>
              </div>
            </div>
          </div>
        </div>

        <div className="relative z-10 space-y-4">
          <blockquote className="text-lg leading-relaxed text-sidebar-foreground/80">
            &ldquo;Conecte seus marketplaces, consolide suas vendas e tenha
            controle total das suas finanças em um só lugar.&rdquo;
          </blockquote>
        </div>

        <div className="relative z-10 flex gap-6 text-xs text-sidebar-foreground/40">
          <span>© 2026 Brasaller</span>
          <span>Clarituz Development</span>
        </div>
      </div>

      <div className="flex flex-1 items-center justify-center bg-background p-8">
        <div className="w-full max-w-sm">{children}</div>
      </div>
    </div>
  );
}