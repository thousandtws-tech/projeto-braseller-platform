"use client"

import { useState } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

const mockLancamentos = [
  { id: "LAN-001", tipo: "venda", marketplace: "Mercado Livre", descricao: "Smartphone Samsung Galaxy", valor: 1899.90, taxas: 189.99, liquido: 1709.91, data: "2024-01-15", status: "recebido" },
  { id: "LAN-002", tipo: "venda", marketplace: "Shopee", descricao: "Fone Bluetooth JBL", valor: 299.90, taxas: 44.99, liquido: 254.91, data: "2024-01-15", status: "pendente" },
  { id: "LAN-003", tipo: "venda", marketplace: "Amazon", descricao: "Notebook Dell Inspiron", valor: 3499.00, taxas: 524.85, liquido: 2974.15, data: "2024-01-14", status: "recebido" },
  { id: "LAN-004", tipo: "venda", marketplace: "Mercado Livre", descricao: "Smart TV LG 50\"", valor: 2199.00, taxas: 219.90, liquido: 1979.10, data: "2024-01-14", status: "pendente" },
  { id: "LAN-005", tipo: "estorno", marketplace: "Shopee", descricao: "Devolução - Capa Celular", valor: -49.90, taxas: 0, liquido: -49.90, data: "2024-01-13", status: "processado" },
]

export default function LancamentosPage() {
  const [filtroMarketplace, setFiltroMarketplace] = useState("")
  const [filtroStatus, setFiltroStatus] = useState("")

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Lançamentos</h1>
          <p className="text-muted-foreground">Gerencie suas vendas e movimentações dos marketplaces</p>
        </div>
        <Button variant="outline" className="w-fit">
          <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
          </svg>
          Exportar CSV
        </Button>
      </div>

      {/* Filtros */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Filtros</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-4">
            <div className="space-y-2">
              <Label htmlFor="periodo">Período</Label>
              <Input id="periodo" type="date" className="h-10" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="marketplace">Marketplace</Label>
              <select 
                id="marketplace"
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                value={filtroMarketplace}
                onChange={(e) => setFiltroMarketplace(e.target.value)}
              >
                <option value="">Todos</option>
                <option value="mercadolivre">Mercado Livre</option>
                <option value="shopee">Shopee</option>
                <option value="amazon">Amazon</option>
                <option value="magalu">Magalu</option>
              </select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="status">Status</Label>
              <select 
                id="status"
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                value={filtroStatus}
                onChange={(e) => setFiltroStatus(e.target.value)}
              >
                <option value="">Todos</option>
                <option value="recebido">Recebido</option>
                <option value="pendente">Pendente</option>
                <option value="processado">Processado</option>
              </select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="busca">Buscar</Label>
              <Input id="busca" placeholder="ID ou descrição..." className="h-10" />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Resumo */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardContent className="pt-6">
            <div className="text-sm text-muted-foreground">Total Vendas</div>
            <div className="text-2xl font-bold">R$ 7.897,80</div>
            <div className="text-xs text-muted-foreground mt-1">5 lançamentos</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-sm text-muted-foreground">Total Taxas</div>
            <div className="text-2xl font-bold">R$ 979,73</div>
            <div className="text-xs text-muted-foreground mt-1">12.4% média</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-sm text-muted-foreground">Valor Líquido</div>
            <div className="text-2xl font-bold">R$ 6.868,17</div>
            <div className="text-xs text-muted-foreground mt-1">Após taxas</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-sm text-muted-foreground">A Receber</div>
            <div className="text-2xl font-bold">R$ 2.234,01</div>
            <div className="text-xs text-muted-foreground mt-1">2 pendentes</div>
          </CardContent>
        </Card>
      </div>

      {/* Tabela de Lançamentos */}
      <Card>
        <CardHeader>
          <CardTitle>Histórico de Lançamentos</CardTitle>
          <CardDescription>Todas as movimentações sincronizadas dos marketplaces</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  <th className="text-left py-3 px-2 font-medium text-muted-foreground">ID</th>
                  <th className="text-left py-3 px-2 font-medium text-muted-foreground">Data</th>
                  <th className="text-left py-3 px-2 font-medium text-muted-foreground">Marketplace</th>
                  <th className="text-left py-3 px-2 font-medium text-muted-foreground">Descrição</th>
                  <th className="text-right py-3 px-2 font-medium text-muted-foreground">Valor Bruto</th>
                  <th className="text-right py-3 px-2 font-medium text-muted-foreground">Taxas</th>
                  <th className="text-right py-3 px-2 font-medium text-muted-foreground">Líquido</th>
                  <th className="text-left py-3 px-2 font-medium text-muted-foreground">Status</th>
                </tr>
              </thead>
              <tbody>
                {mockLancamentos.map((lanc) => (
                  <tr key={lanc.id} className="border-b border-border hover:bg-muted/50">
                    <td className="py-3 px-2 font-mono text-xs">{lanc.id}</td>
                    <td className="py-3 px-2">{new Date(lanc.data).toLocaleDateString("pt-BR")}</td>
                    <td className="py-3 px-2">
                      <span className="inline-flex items-center px-2 py-1 rounded-md bg-muted text-xs">
                        {lanc.marketplace}
                      </span>
                    </td>
                    <td className="py-3 px-2 max-w-[200px] truncate">{lanc.descricao}</td>
                    <td className={`py-3 px-2 text-right ${lanc.valor < 0 ? "text-destructive" : ""}`}>
                      R$ {lanc.valor.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}
                    </td>
                    <td className="py-3 px-2 text-right text-muted-foreground">
                      R$ {lanc.taxas.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}
                    </td>
                    <td className={`py-3 px-2 text-right font-medium ${lanc.liquido < 0 ? "text-destructive" : ""}`}>
                      R$ {lanc.liquido.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}
                    </td>
                    <td className="py-3 px-2">
                      <span className={`inline-flex items-center px-2 py-1 rounded-md text-xs ${
                        lanc.status === "recebido" ? "bg-muted" :
                        lanc.status === "pendente" ? "bg-muted border border-dashed border-border" :
                        "bg-muted"
                      }`}>
                        {lanc.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          
          {/* Paginação */}
          <div className="flex items-center justify-between mt-4 pt-4 border-t border-border">
            <p className="text-sm text-muted-foreground">Mostrando 1-5 de 5 resultados</p>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" disabled>Anterior</Button>
              <Button variant="outline" size="sm" disabled>Próximo</Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
