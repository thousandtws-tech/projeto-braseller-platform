"use client"

import { useState } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

const mockDespesas = [
  { id: "DES-001", categoria: "Embalagens", descricao: "Caixas de papelão 30x20x15", valor: 450.00, data: "2024-01-15", recorrente: false },
  { id: "DES-002", categoria: "Frete", descricao: "Correios - envios Janeiro", valor: 1280.50, data: "2024-01-14", recorrente: false },
  { id: "DES-003", categoria: "Marketing", descricao: "Ads Mercado Livre", valor: 800.00, data: "2024-01-13", recorrente: true },
  { id: "DES-004", categoria: "Funcionários", descricao: "Salário - Assistente", valor: 2500.00, data: "2024-01-10", recorrente: true },
  { id: "DES-005", categoria: "Software", descricao: "Assinatura ERP", valor: 199.90, data: "2024-01-05", recorrente: true },
  { id: "DES-006", categoria: "Impostos", descricao: "DAS Simples Nacional", valor: 890.00, data: "2024-01-20", recorrente: true },
]

const categorias = [
  "Embalagens",
  "Frete",
  "Marketing",
  "Funcionários",
  "Software",
  "Impostos",
  "Aluguel",
  "Outros"
]

export default function DespesasPage() {
  const [showModal, setShowModal] = useState(false)

  const totalDespesas = mockDespesas.reduce((acc, d) => acc + d.valor, 0)
  const despesasRecorrentes = mockDespesas.filter(d => d.recorrente).reduce((acc, d) => acc + d.valor, 0)
  const despesasVariaveis = mockDespesas.filter(d => !d.recorrente).reduce((acc, d) => acc + d.valor, 0)

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Despesas</h1>
          <p className="text-muted-foreground">Controle seus custos operacionais e despesas fixas</p>
        </div>
        <Button onClick={() => setShowModal(true)}>
          <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Nova Despesa
        </Button>
      </div>

      {/* Resumo */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardContent className="pt-6">
            <div className="text-sm text-muted-foreground">Total do Mês</div>
            <div className="text-2xl font-bold">R$ {totalDespesas.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</div>
            <div className="text-xs text-muted-foreground mt-1">{mockDespesas.length} despesas</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-sm text-muted-foreground">Recorrentes</div>
            <div className="text-2xl font-bold">R$ {despesasRecorrentes.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</div>
            <div className="text-xs text-muted-foreground mt-1">Despesas fixas</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-sm text-muted-foreground">Variáveis</div>
            <div className="text-2xl font-bold">R$ {despesasVariaveis.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</div>
            <div className="text-xs text-muted-foreground mt-1">Despesas pontuais</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-sm text-muted-foreground">Maior Categoria</div>
            <div className="text-2xl font-bold">Funcionários</div>
            <div className="text-xs text-muted-foreground mt-1">40.8% do total</div>
          </CardContent>
        </Card>
      </div>

      {/* Gráfico por Categoria */}
      <Card>
        <CardHeader>
          <CardTitle>Despesas por Categoria</CardTitle>
          <CardDescription>Distribuição dos custos no período</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {[
              { nome: "Funcionários", valor: 2500, percent: 40.8 },
              { nome: "Frete", valor: 1280.5, percent: 20.9 },
              { nome: "Impostos", valor: 890, percent: 14.5 },
              { nome: "Marketing", valor: 800, percent: 13.1 },
              { nome: "Embalagens", valor: 450, percent: 7.3 },
              { nome: "Software", valor: 199.9, percent: 3.3 },
            ].map((cat) => (
              <div key={cat.nome} className="space-y-2">
                <div className="flex justify-between text-sm">
                  <span>{cat.nome}</span>
                  <span className="text-muted-foreground">R$ {cat.valor.toLocaleString("pt-BR", { minimumFractionDigits: 2 })} ({cat.percent}%)</span>
                </div>
                <div className="h-2 bg-muted rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-foreground/30 rounded-full" 
                    style={{ width: `${cat.percent}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Tabela de Despesas */}
      <Card>
        <CardHeader>
          <CardTitle>Histórico de Despesas</CardTitle>
          <CardDescription>Todas as despesas registradas</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  <th className="text-left py-3 px-2 font-medium text-muted-foreground">ID</th>
                  <th className="text-left py-3 px-2 font-medium text-muted-foreground">Data</th>
                  <th className="text-left py-3 px-2 font-medium text-muted-foreground">Categoria</th>
                  <th className="text-left py-3 px-2 font-medium text-muted-foreground">Descrição</th>
                  <th className="text-right py-3 px-2 font-medium text-muted-foreground">Valor</th>
                  <th className="text-left py-3 px-2 font-medium text-muted-foreground">Tipo</th>
                  <th className="text-left py-3 px-2 font-medium text-muted-foreground">Ações</th>
                </tr>
              </thead>
              <tbody>
                {mockDespesas.map((desp) => (
                  <tr key={desp.id} className="border-b border-border hover:bg-muted/50">
                    <td className="py-3 px-2 font-mono text-xs">{desp.id}</td>
                    <td className="py-3 px-2">{new Date(desp.data).toLocaleDateString("pt-BR")}</td>
                    <td className="py-3 px-2">
                      <span className="inline-flex items-center px-2 py-1 rounded-md bg-muted text-xs">
                        {desp.categoria}
                      </span>
                    </td>
                    <td className="py-3 px-2">{desp.descricao}</td>
                    <td className="py-3 px-2 text-right font-medium">
                      R$ {desp.valor.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}
                    </td>
                    <td className="py-3 px-2">
                      {desp.recorrente ? (
                        <span className="inline-flex items-center px-2 py-1 rounded-md bg-muted border border-dashed border-border text-xs">
                          Recorrente
                        </span>
                      ) : (
                        <span className="inline-flex items-center px-2 py-1 rounded-md bg-muted text-xs">
                          Pontual
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-2">
                      <div className="flex gap-1">
                        <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                          <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                          </svg>
                        </Button>
                        <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                          <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                          </svg>
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Modal Nova Despesa */}
      {showModal && (
        <div className="fixed inset-0 z-50 bg-foreground/20 flex items-center justify-center p-4">
          <Card className="w-full max-w-md">
            <CardHeader>
              <CardTitle>Nova Despesa</CardTitle>
              <CardDescription>Adicione uma nova despesa ao sistema</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="categoria">Categoria</Label>
                <select 
                  id="categoria"
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                >
                  {categorias.map((cat) => (
                    <option key={cat} value={cat}>{cat}</option>
                  ))}
                </select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="descricao">Descrição</Label>
                <Input id="descricao" placeholder="Descrição da despesa" />
              </div>
              <div className="space-y-2">
                <Label htmlFor="valor">Valor</Label>
                <Input id="valor" type="number" placeholder="0,00" />
              </div>
              <div className="space-y-2">
                <Label htmlFor="data">Data</Label>
                <Input id="data" type="date" />
              </div>
              <div className="flex items-center gap-2">
                <input type="checkbox" id="recorrente" className="rounded border-input" />
                <Label htmlFor="recorrente" className="font-normal">Despesa recorrente (mensal)</Label>
              </div>
              <div className="flex gap-2 pt-4">
                <Button variant="outline" className="flex-1" onClick={() => setShowModal(false)}>
                  Cancelar
                </Button>
                <Button className="flex-1" onClick={() => setShowModal(false)}>
                  Salvar
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  )
}
