"use client"

import { useState } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"

const conectores = [
  { 
    id: "mercadolivre", 
    nome: "Mercado Livre", 
    status: "conectado", 
    ultimaSync: "2024-01-15T14:30:00",
    vendas: 156,
    receita: 28450.00,
    conta: "loja_exemplo_ml"
  },
  { 
    id: "shopee", 
    nome: "Shopee", 
    status: "conectado", 
    ultimaSync: "2024-01-15T14:25:00",
    vendas: 89,
    receita: 12340.50,
    conta: "loja_exemplo_shopee"
  },
  { 
    id: "amazon", 
    nome: "Amazon", 
    status: "pendente", 
    ultimaSync: null,
    vendas: 0,
    receita: 0,
    conta: null
  },
  { 
    id: "magalu", 
    nome: "Magazine Luiza", 
    status: "erro", 
    ultimaSync: "2024-01-14T10:00:00",
    vendas: 23,
    receita: 5100.00,
    conta: "loja_exemplo_magalu",
    erro: "Token expirado. Reconecte sua conta."
  },
  { 
    id: "americanas", 
    nome: "Americanas", 
    status: "desconectado", 
    ultimaSync: null,
    vendas: 0,
    receita: 0,
    conta: null
  },
  { 
    id: "shein", 
    nome: "Shein", 
    status: "desconectado", 
    ultimaSync: null,
    vendas: 0,
    receita: 0,
    conta: null
  },
]

export default function ConectoresPage() {
  const [syncing, setSyncing] = useState<string | null>(null)

  const handleSync = (id: string) => {
    setSyncing(id)
    setTimeout(() => setSyncing(null), 2000)
  }

  const conectoresAtivos = conectores.filter(c => c.status === "conectado").length
  const totalVendas = conectores.reduce((acc, c) => acc + c.vendas, 0)
  const totalReceita = conectores.reduce((acc, c) => acc + c.receita, 0)

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Conectores</h1>
          <p className="text-muted-foreground">Gerencie suas integrações com marketplaces</p>
        </div>
        <Button variant="outline">
          <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          Sincronizar Todos
        </Button>
      </div>

      {/* Resumo */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardContent className="pt-6">
            <div className="text-sm text-muted-foreground">Conectores Ativos</div>
            <div className="text-2xl font-bold">{conectoresAtivos}</div>
            <div className="text-xs text-muted-foreground mt-1">de {conectores.length} disponíveis</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-sm text-muted-foreground">Total de Vendas</div>
            <div className="text-2xl font-bold">{totalVendas}</div>
            <div className="text-xs text-muted-foreground mt-1">Este mês</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-sm text-muted-foreground">Receita Total</div>
            <div className="text-2xl font-bold">R$ {totalReceita.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</div>
            <div className="text-xs text-muted-foreground mt-1">De todos os marketplaces</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-sm text-muted-foreground">Última Sincronização</div>
            <div className="text-2xl font-bold">14:30</div>
            <div className="text-xs text-muted-foreground mt-1">Há 5 minutos</div>
          </CardContent>
        </Card>
      </div>

      {/* Lista de Conectores */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {conectores.map((conector) => (
          <Card key={conector.id} className={conector.status === "erro" ? "border-dashed" : ""}>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <CardTitle className="text-base">{conector.nome}</CardTitle>
                <span className={`inline-flex items-center px-2 py-1 rounded-md text-xs ${
                  conector.status === "conectado" ? "bg-muted" :
                  conector.status === "pendente" ? "bg-muted border border-dashed border-border" :
                  conector.status === "erro" ? "bg-muted border border-dashed border-border" :
                  "bg-muted text-muted-foreground"
                }`}>
                  {conector.status === "conectado" && "Conectado"}
                  {conector.status === "pendente" && "Pendente"}
                  {conector.status === "erro" && "Erro"}
                  {conector.status === "desconectado" && "Desconectado"}
                </span>
              </div>
              {conector.conta && (
                <CardDescription className="font-mono text-xs">{conector.conta}</CardDescription>
              )}
            </CardHeader>
            <CardContent className="space-y-4">
              {conector.status === "conectado" && (
                <>
                  <div className="grid grid-cols-2 gap-4 text-sm">
                    <div>
                      <p className="text-muted-foreground">Vendas</p>
                      <p className="font-medium">{conector.vendas}</p>
                    </div>
                    <div>
                      <p className="text-muted-foreground">Receita</p>
                      <p className="font-medium">R$ {conector.receita.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</p>
                    </div>
                  </div>
                  {conector.ultimaSync && (
                    <p className="text-xs text-muted-foreground">
                      Última sync: {new Date(conector.ultimaSync).toLocaleString("pt-BR")}
                    </p>
                  )}
                  <div className="flex gap-2">
                    <Button 
                      variant="outline" 
                      size="sm" 
                      className="flex-1"
                      onClick={() => handleSync(conector.id)}
                      disabled={syncing === conector.id}
                    >
                      {syncing === conector.id ? (
                        <>
                          <svg className="animate-spin h-4 w-4 mr-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                          </svg>
                          Sincronizando
                        </>
                      ) : (
                        <>
                          <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                          </svg>
                          Sincronizar
                        </>
                      )}
                    </Button>
                    <Button variant="outline" size="sm">
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                    </Button>
                  </div>
                </>
              )}

              {conector.status === "erro" && (
                <>
                  <div className="p-3 bg-muted rounded-md border border-dashed border-border">
                    <p className="text-sm">{conector.erro}</p>
                  </div>
                  <Button variant="outline" size="sm" className="w-full">
                    Reconectar
                  </Button>
                </>
              )}

              {conector.status === "pendente" && (
                <Button size="sm" className="w-full">
                  Completar Configuração
                </Button>
              )}

              {conector.status === "desconectado" && (
                <Button variant="outline" size="sm" className="w-full">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                  </svg>
                  Conectar
                </Button>
              )}
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Como Funciona */}
      <Card>
        <CardHeader>
          <CardTitle>Como funciona a integração</CardTitle>
          <CardDescription>Conecte seus marketplaces em 3 passos simples</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-6 md:grid-cols-3">
            <div className="flex gap-4">
              <div className="w-10 h-10 rounded-full bg-muted border border-border flex items-center justify-center shrink-0">
                <span className="font-medium">1</span>
              </div>
              <div>
                <h4 className="font-medium">Autorize a conexão</h4>
                <p className="text-sm text-muted-foreground">Clique em conectar e autorize o acesso à sua conta do marketplace</p>
              </div>
            </div>
            <div className="flex gap-4">
              <div className="w-10 h-10 rounded-full bg-muted border border-border flex items-center justify-center shrink-0">
                <span className="font-medium">2</span>
              </div>
              <div>
                <h4 className="font-medium">Sincronização automática</h4>
                <p className="text-sm text-muted-foreground">Suas vendas e taxas serão importadas automaticamente</p>
              </div>
            </div>
            <div className="flex gap-4">
              <div className="w-10 h-10 rounded-full bg-muted border border-border flex items-center justify-center shrink-0">
                <span className="font-medium">3</span>
              </div>
              <div>
                <h4 className="font-medium">Acompanhe em tempo real</h4>
                <p className="text-sm text-muted-foreground">Visualize todas as movimentações no dashboard unificado</p>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
