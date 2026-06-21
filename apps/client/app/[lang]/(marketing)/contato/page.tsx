import { Mail, MapPin, Phone } from 'lucide-react'

import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'

interface Props {
  params: Promise<{ lang: Locale }>
}

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.marketing.nav.contato }
}

const CHANNELS = [
  { icon: Mail, label: 'E-mail', value: 'contato@brasaller.com.br' },
  { icon: Phone, label: 'Telefone', value: '+55 (00) 0000-0000' },
  { icon: MapPin, label: 'Endereço', value: 'São Paulo, SP — Brasil' },
]

export default async function ContatoPage({ params }: Props) {
  void params

  return (
    <div className="mx-auto max-w-6xl space-y-12 px-6 py-16">
      <section className="max-w-2xl space-y-4">
        <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">Contato</h1>
        <p className="text-base text-slate-500">
          Fale com o nosso time de vendas ou suporte. Wireframe — formulário ainda não conectado a um endpoint.
        </p>
      </section>

      <section className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="text-base text-slate-900">Envie uma mensagem</CardTitle>
          </CardHeader>
          <CardContent>
            <form className="space-y-4">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="space-y-1.5">
                  <Label htmlFor="name">Nome</Label>
                  <Input id="name" name="name" placeholder="Seu nome" disabled />
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="email">E-mail</Label>
                  <Input id="email" name="email" type="email" placeholder="seu@email.com" disabled />
                </div>
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="subject">Assunto</Label>
                <Input id="subject" name="subject" placeholder="Como podemos ajudar?" disabled />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="message">Mensagem</Label>
                <textarea
                  id="message"
                  name="message"
                  rows={5}
                  placeholder="Escreva sua mensagem"
                  disabled
                  className="flex w-full rounded-lg border border-border bg-background px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground disabled:cursor-not-allowed disabled:opacity-50"
                />
              </div>
              <Button type="submit" disabled>
                Enviar mensagem
              </Button>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base text-slate-900">Canais de atendimento</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {CHANNELS.map(({ icon: Icon, label, value }) => (
              <div key={label} className="flex items-start gap-3">
                <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10">
                  <Icon className="size-4 text-primary" />
                </div>
                <div>
                  <p className="text-sm font-medium text-slate-900">{label}</p>
                  <p className="text-sm text-slate-500">{value}</p>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      </section>
    </div>
  )
}
