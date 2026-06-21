import Link from 'next/link'
import { Check } from 'lucide-react'

import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/shared/ui/card'
import { cn } from '@/shared/lib/utils'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'

interface Props {
  params: Promise<{ lang: Locale }>
}

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.marketing.nav.planos }
}

const PLANS = [
  {
    code: 'starter',
    name: 'Starter',
    price: 'R$ 0',
    period: '/mês',
    description: 'Para quem está começando a vender em marketplaces.',
    highlighted: false,
    features: ['1 marketplace conectado', 'Dashboard financeiro', 'Extrato e conciliação básica'],
  },
  {
    code: 'pro',
    name: 'Profissional',
    price: 'R$ 99',
    period: '/mês',
    description: 'Para operações em crescimento com múltiplos canais.',
    highlighted: true,
    features: ['Marketplaces ilimitados', 'DRE e balanço patrimonial', 'Monitoramento de APIs', 'Relatórios para o contador'],
  },
  {
    code: 'enterprise',
    name: 'Enterprise',
    price: 'Sob consulta',
    period: '',
    description: 'Para operações BPO e múltiplos clientes/tenants.',
    highlighted: false,
    features: ['Tudo do Profissional', 'Acesso multi-tenant (BPO)', 'Suporte dedicado', 'SLA personalizado'],
  },
]

export default async function PlanosPage({ params }: Props) {
  const { lang } = await params

  return (
    <div className="mx-auto max-w-6xl space-y-12 px-6 py-16">
      <section className="max-w-2xl space-y-4">
        <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">Planos</h1>
        <p className="text-base text-slate-500">
          Escolha o plano ideal para o seu estágio de operação. Wireframe — valores e limites ilustrativos.
        </p>
      </section>

      <section className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {PLANS.map((plan) => (
          <Card key={plan.code} className={cn('flex flex-col', plan.highlighted && 'border-primary shadow-md')}>
            <CardHeader className="space-y-2 pb-2">
              <div className="flex items-center justify-between">
                <CardTitle className="text-base text-slate-900">{plan.name}</CardTitle>
                {plan.highlighted && <Badge>Mais popular</Badge>}
              </div>
              <p className="text-sm text-slate-500">{plan.description}</p>
              <p className="pt-2">
                <span className="text-3xl font-bold text-slate-900">{plan.price}</span>
                <span className="text-sm text-slate-500">{plan.period}</span>
              </p>
            </CardHeader>
            <CardContent className="flex-1">
              <ul className="space-y-2 text-sm text-slate-600">
                {plan.features.map((feature) => (
                  <li key={feature} className="flex items-start gap-2">
                    <Check className="mt-0.5 size-4 shrink-0 text-emerald-600" />
                    {feature}
                  </li>
                ))}
              </ul>
            </CardContent>
            <CardFooter>
              <Button
                className="w-full"
                variant={plan.highlighted ? 'default' : 'outline'}
                render={<Link href={`/${lang}/register`} />}
              >
                Começar agora
              </Button>
            </CardFooter>
          </Card>
        ))}
      </section>

      <section className="rounded-2xl border border-slate-200 bg-slate-50/60 p-10">
        <h2 className="text-2xl font-semibold text-slate-900">Perguntas frequentes</h2>
        <p className="mt-2 max-w-2xl text-sm text-slate-500">
          Wireframe — espaço reservado para FAQ sobre cobrança, cancelamento e troca de planos.
        </p>
      </section>
    </div>
  )
}
