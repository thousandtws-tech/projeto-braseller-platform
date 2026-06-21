import Link from 'next/link'
import {
  Activity,
  ArrowRight,
  BarChart3,
  Landmark,
  Scale,
  ShoppingCart,
  Store,
} from 'lucide-react'

import { Button } from '@/shared/ui/button'
import { Badge } from '@/shared/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'

interface Props {
  params: Promise<{ lang: Locale }>
}

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.marketing.nav.inicio }
}

const FEATURES = [
  { icon: Store, title: 'Conectores de marketplace', description: 'Mercado Livre, Shopee, Amazon e outros canais em um só painel.' },
  { icon: BarChart3, title: 'DRE em tempo real', description: 'Demonstrativo de resultado consolidado, atualizado automaticamente.' },
  { icon: Landmark, title: 'Extrato e conciliação', description: 'Importe extratos bancários e concilie lançamentos automaticamente.' },
  { icon: Scale, title: 'Balanço patrimonial', description: 'Visão consolidada de ativos, passivos e patrimônio líquido.' },
  { icon: Activity, title: 'Monitoramento de APIs', description: 'Disponibilidade e integridade das integrações em tempo real.' },
  { icon: ShoppingCart, title: 'Gestão de pedidos', description: 'Acompanhe vendas, pagamentos e taxas de todos os canais.' },
]

const STEPS = [
  { step: '1', title: 'Conecte seus marketplaces', description: 'Autorize o acesso às suas contas em poucos cliques.' },
  { step: '2', title: 'Sincronize seus dados', description: 'Pedidos, pagamentos e taxas são importados automaticamente.' },
  { step: '3', title: 'Acompanhe seus resultados', description: 'Dashboards, DRE e relatórios prontos para o seu contador.' },
]

export default async function InicioPage({ params }: Props) {
  const { lang } = await params

  return (
    <div className="mx-auto max-w-6xl space-y-24 px-6 py-16">
      {/* Hero */}
      <section className="grid grid-cols-1 items-center gap-10 lg:grid-cols-2">
        <div className="space-y-6">
          <Badge variant="secondary" className="rounded-full px-3 py-1 text-xs">
            Gestão financeira para marketplaces
          </Badge>
          <h1 className="text-4xl font-bold tracking-tight text-slate-900 sm:text-5xl">
            Consolide as finanças dos seus marketplaces em um só lugar
          </h1>
          <p className="max-w-lg text-base text-slate-500">
            Conecte Mercado Livre, Shopee, Amazon e outros canais e acompanhe
            vendas, taxas, DRE e fluxo de caixa em tempo real.
          </p>
          <div className="flex flex-wrap items-center gap-3">
            <Button size="lg" render={<Link href={`/${lang}/register`} />}>
              Criar conta gratuita
              <ArrowRight className="size-4" data-icon="inline-end" />
            </Button>
            <Button size="lg" variant="outline" render={<Link href={`/${lang}/planos`} />}>
              Ver planos
            </Button>
          </div>
        </div>

        <Card className="border-slate-200 bg-slate-50/60 p-2">
          <CardContent className="flex aspect-video items-center justify-center rounded-xl bg-white text-sm text-slate-400">
            Wireframe — visual do produto
          </CardContent>
        </Card>
      </section>

      {/* Recursos */}
      <section className="space-y-8">
        <div className="max-w-xl space-y-2">
          <h2 className="text-2xl font-semibold text-slate-900">Recursos</h2>
          <p className="text-sm text-slate-500">
            Tudo o que você precisa para gerenciar a operação financeira do seu negócio em marketplaces.
          </p>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map(({ icon: Icon, title, description }) => (
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

      {/* Como funciona */}
      <section className="space-y-8">
        <div className="max-w-xl space-y-2">
          <h2 className="text-2xl font-semibold text-slate-900">Como funciona</h2>
        </div>

        <div className="grid grid-cols-1 gap-6 sm:grid-cols-3">
          {STEPS.map(({ step, title, description }) => (
            <div key={step} className="space-y-3">
              <div className="flex size-9 items-center justify-center rounded-full bg-blue-50 text-sm font-semibold text-blue-700">
                {step}
              </div>
              <h3 className="text-sm font-semibold text-slate-900">{title}</h3>
              <p className="text-sm text-slate-500">{description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* CTA final */}
      <section className="rounded-2xl border border-slate-200 bg-slate-50/60 p-10 text-center">
        <h2 className="text-2xl font-semibold text-slate-900">Pronto para começar?</h2>
        <p className="mx-auto mt-2 max-w-md text-sm text-slate-500">
          Crie sua conta gratuita e conecte seu primeiro marketplace em minutos.
        </p>
        <div className="mt-6 flex justify-center gap-3">
          <Button size="lg" render={<Link href={`/${lang}/register`} />}>
            Criar conta gratuita
          </Button>
          <Button size="lg" variant="outline" render={<Link href={`/${lang}/contato`} />}>
            Falar com vendas
          </Button>
        </div>
      </section>
    </div>
  )
}
