import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Compass, Heart, ShieldCheck, Target } from 'lucide-react'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'

interface Props {
  params: Promise<{ lang: Locale }>
}

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.marketing.nav.sobre }
}

const VALUES = [
  { icon: Target, title: 'Missão', description: 'Simplificar a gestão financeira de quem vende em marketplaces.' },
  { icon: Compass, title: 'Visão', description: 'Ser a plataforma de referência em conciliação multi-canal no Brasil.' },
  { icon: Heart, title: 'Valores', description: 'Transparência, automação e foco no resultado do cliente.' },
  { icon: ShieldCheck, title: 'Segurança', description: 'Dados financeiros protegidos com práticas de mercado e auditoria contínua.' },
]

export default async function SobrePage({ params }: Props) {
  void params

  return (
    <div className="mx-auto max-w-6xl space-y-16 px-6 py-16">
      <section className="max-w-2xl space-y-4">
        <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">Sobre a Brasaller</h1>
        <p className="text-base text-slate-500">
          A Brasaller nasceu para resolver um problema comum a quem vende em múltiplos marketplaces:
          consolidar vendas, taxas e pagamentos de canais diferentes em uma única visão financeira.
        </p>
      </section>

      <section className="space-y-8">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {VALUES.map(({ icon: Icon, title, description }) => (
            <Card key={title}>
              <CardHeader className="flex-row items-center gap-3 pb-2">
                <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10">
                  <Icon className="size-4 text-primary" />
                </div>
                <CardTitle className="text-sm">{title}</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-slate-500">{description}</p>
              </CardContent>
            </Card>
          ))}
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-slate-50/60 p-10">
        <h2 className="text-2xl font-semibold text-slate-900">Nossa história</h2>
        <p className="mt-2 max-w-2xl text-sm text-slate-500">
          Wireframe — espaço reservado para a história da empresa, equipe e marcos.
        </p>
      </section>
    </div>
  )
}
