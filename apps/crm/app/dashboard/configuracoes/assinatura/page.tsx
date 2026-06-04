"use client"

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"

const planos = [
  { 
    id: "starter", 
    nome: "Starter", 
    preco: 49.90, 
    limiteConectores: 2, 
    limiteUsuarios: 1,
    recursos: ["2 conectores", "1 usuário", "Relatórios básicos", "Suporte por email"]
  },
  { 
    id: "professional", 
    nome: "Professional", 
    preco: 149.90, 
    limiteConectores: 5, 
    limiteUsuarios: 3,
    recursos: ["5 conectores", "3 usuários", "DRE completo", "Acesso contador", "Suporte prioritário"],
    popular: true
  },
  { 
    id: "enterprise", 
    nome: "Enterprise", 
    preco: 349.90, 
    limiteConectores: -1, 
    limiteUsuarios: -1,
    recursos: ["Conectores ilimitados", "Usuários ilimitados", "API access", "White-label", "Suporte dedicado"]
  },
]

export default function AssinaturaPage() {
  const planoAtual = "professional"

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Assinatura</h1>
        <p className="text-muted-foreground">Gerencie seu plano e faturamento</p>
      </div>

      {/* Status Atual */}
      <Card>
        <CardHeader>
          <CardTitle>Plano Atual</CardTitle>
          <CardDescription>Detalhes da sua assinatura</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 rounded-lg bg-muted border border-border flex items-center justify-center">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z" />
                </svg>
              </div>
              <div>
                <h3 className="text-xl font-bold">Professional</h3>
                <p className="text-muted-foreground">R$ 149,90/mês</p>
              </div>
            </div>
            <div className="flex flex-col md:items-end gap-1">
              <span className="inline-flex items-center px-3 py-1 rounded-md bg-muted text-sm">
                Ativo
              </span>
              <p className="text-sm text-muted-foreground">Próxima cobrança: 15/02/2024</p>
            </div>
          </div>

          <div className="grid gap-4 md:grid-cols-4 mt-6 pt-6 border-t border-border">
            <div>
              <p className="text-sm text-muted-foreground">Conectores</p>
              <p className="font-medium">3 de 5 usados</p>
              <div className="h-2 bg-muted rounded-full mt-2 overflow-hidden">
                <div className="h-full bg-foreground/30 rounded-full" style={{ width: "60%" }} />
              </div>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Usuários</p>
              <p className="font-medium">2 de 3 usados</p>
              <div className="h-2 bg-muted rounded-full mt-2 overflow-hidden">
                <div className="h-full bg-foreground/30 rounded-full" style={{ width: "66%" }} />
              </div>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Contadores</p>
              <p className="font-medium">1 de 2 usados</p>
              <div className="h-2 bg-muted rounded-full mt-2 overflow-hidden">
                <div className="h-full bg-foreground/30 rounded-full" style={{ width: "50%" }} />
              </div>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Armazenamento</p>
              <p className="font-medium">2.3 GB de 10 GB</p>
              <div className="h-2 bg-muted rounded-full mt-2 overflow-hidden">
                <div className="h-full bg-foreground/30 rounded-full" style={{ width: "23%" }} />
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Planos Disponíveis */}
      <Card>
        <CardHeader>
          <CardTitle>Planos Disponíveis</CardTitle>
          <CardDescription>Escolha o melhor plano para seu negócio</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-3">
            {planos.map((plano) => (
              <div 
                key={plano.id} 
                className={`relative p-6 border rounded-lg ${
                  plano.id === planoAtual 
                    ? "border-foreground/30 bg-muted/30" 
                    : "border-border"
                } ${plano.popular ? "border-dashed border-2" : ""}`}
              >
                {plano.popular && (
                  <span className="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-1 bg-muted border border-border rounded-full text-xs">
                    Mais Popular
                  </span>
                )}
                <div className="text-center mb-4">
                  <h3 className="text-lg font-bold">{plano.nome}</h3>
                  <div className="mt-2">
                    <span className="text-3xl font-bold">R$ {plano.preco.toFixed(2).replace(".", ",")}</span>
                    <span className="text-muted-foreground">/mês</span>
                  </div>
                </div>
                <ul className="space-y-2 mb-6">
                  {plano.recursos.map((recurso, index) => (
                    <li key={index} className="flex items-center gap-2 text-sm">
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                      </svg>
                      {recurso}
                    </li>
                  ))}
                </ul>
                <Button 
                  className="w-full" 
                  variant={plano.id === planoAtual ? "outline" : "default"}
                  disabled={plano.id === planoAtual}
                >
                  {plano.id === planoAtual ? "Plano Atual" : "Selecionar"}
                </Button>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Histórico de Faturas */}
      <Card>
        <CardHeader>
          <CardTitle>Histórico de Faturas</CardTitle>
          <CardDescription>Suas últimas faturas</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  <th className="text-left py-3 px-2 font-medium text-muted-foreground">Data</th>
                  <th className="text-left py-3 px-2 font-medium text-muted-foreground">Descrição</th>
                  <th className="text-right py-3 px-2 font-medium text-muted-foreground">Valor</th>
                  <th className="text-left py-3 px-2 font-medium text-muted-foreground">Status</th>
                  <th className="text-left py-3 px-2 font-medium text-muted-foreground">Ações</th>
                </tr>
              </thead>
              <tbody>
                {[
                  { data: "15/01/2024", desc: "Plano Professional - Janeiro", valor: 149.90, status: "pago" },
                  { data: "15/12/2023", desc: "Plano Professional - Dezembro", valor: 149.90, status: "pago" },
                  { data: "15/11/2023", desc: "Plano Professional - Novembro", valor: 149.90, status: "pago" },
                  { data: "15/10/2023", desc: "Plano Starter - Outubro", valor: 49.90, status: "pago" },
                ].map((fatura, index) => (
                  <tr key={index} className="border-b border-border">
                    <td className="py-3 px-2">{fatura.data}</td>
                    <td className="py-3 px-2">{fatura.desc}</td>
                    <td className="py-3 px-2 text-right font-medium">
                      R$ {fatura.valor.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}
                    </td>
                    <td className="py-3 px-2">
                      <span className="inline-flex items-center px-2 py-1 rounded-md bg-muted text-xs">
                        {fatura.status === "pago" ? "Pago" : "Pendente"}
                      </span>
                    </td>
                    <td className="py-3 px-2">
                      <Button variant="ghost" size="sm">
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                        PDF
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Método de Pagamento */}
      <Card>
        <CardHeader>
          <CardTitle>Método de Pagamento</CardTitle>
          <CardDescription>Cartão cadastrado para cobrança</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-between p-4 border border-border rounded-lg">
            <div className="flex items-center gap-4">
              <div className="w-12 h-8 bg-muted border border-border rounded flex items-center justify-center">
                <span className="text-xs font-mono">VISA</span>
              </div>
              <div>
                <p className="font-medium">**** **** **** 4242</p>
                <p className="text-sm text-muted-foreground">Expira em 12/2025</p>
              </div>
            </div>
            <Button variant="outline">Alterar</Button>
          </div>
        </CardContent>
      </Card>

      {/* Cancelamento */}
      <Card className="border-dashed">
        <CardContent className="pt-6">
          <div className="flex items-center justify-between">
            <div>
              <h4 className="font-medium">Cancelar Assinatura</h4>
              <p className="text-sm text-muted-foreground">Você pode cancelar a qualquer momento</p>
            </div>
            <Button variant="outline" className="text-destructive">
              Cancelar Plano
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
