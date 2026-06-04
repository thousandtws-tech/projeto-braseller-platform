"use client"

import { useState } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"

export default function DREPage() {
  const [periodo, setPeriodo] = useState("janeiro-2024")

  const dreData = {
    receitaBruta: 45890.00,
    devolucoes: -1250.00,
    receitaLiquida: 44640.00,
    custoMercadoria: -22320.00,
    taxasMarketplace: -5506.80,
    lucroBruto: 16813.20,
    despesasOperacionais: {
      marketing: -800.00,
      frete: -1280.50,
      embalagens: -450.00,
      software: -199.90,
      funcionarios: -2500.00,
      total: -5230.40
    },
    lucroOperacional: 11582.80,
    impostos: -890.00,
    lucroLiquido: 10692.80,
    margemBruta: 37.66,
    margemLiquida: 23.95
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">DRE - Demonstração do Resultado</h1>
          <p className="text-muted-foreground">Análise detalhada do resultado do exercício</p>
        </div>
        <div className="flex gap-2">
          <div className="space-y-1">
            <Label htmlFor="periodo" className="sr-only">Período</Label>
            <select 
              id="periodo"
              className="flex h-10 rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={periodo}
              onChange={(e) => setPeriodo(e.target.value)}
            >
              <option value="janeiro-2024">Janeiro 2024</option>
              <option value="dezembro-2023">Dezembro 2023</option>
              <option value="novembro-2023">Novembro 2023</option>
              <option value="q4-2023">Q4 2023</option>
              <option value="ano-2023">Ano 2023</option>
            </select>
          </div>
          <Button variant="outline">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            Exportar PDF
          </Button>
        </div>
      </div>

      {/* Cards de Resumo */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardContent className="pt-6">
            <div className="text-sm text-muted-foreground">Receita Líquida</div>
            <div className="text-2xl font-bold">R$ {dreData.receitaLiquida.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-sm text-muted-foreground">Lucro Bruto</div>
            <div className="text-2xl font-bold">R$ {dreData.lucroBruto.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</div>
            <div className="text-xs text-muted-foreground mt-1">Margem: {dreData.margemBruta}%</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-sm text-muted-foreground">Lucro Líquido</div>
            <div className="text-2xl font-bold">R$ {dreData.lucroLiquido.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</div>
            <div className="text-xs text-muted-foreground mt-1">Margem: {dreData.margemLiquida}%</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-sm text-muted-foreground">Total Despesas</div>
            <div className="text-2xl font-bold">R$ {Math.abs(dreData.despesasOperacionais.total).toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</div>
          </CardContent>
        </Card>
      </div>

      {/* DRE Completo */}
      <Card>
        <CardHeader>
          <CardTitle>Demonstração do Resultado do Exercício</CardTitle>
          <CardDescription>Período: Janeiro 2024</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-1">
            {/* Receita */}
            <div className="flex justify-between py-3 border-b border-border">
              <span className="font-medium">RECEITA BRUTA DE VENDAS</span>
              <span className="font-mono">R$ {dreData.receitaBruta.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</span>
            </div>
            <div className="flex justify-between py-2 pl-4 text-muted-foreground">
              <span>(-) Devoluções e Cancelamentos</span>
              <span className="font-mono">R$ {dreData.devolucoes.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</span>
            </div>
            <div className="flex justify-between py-3 border-b border-border bg-muted/50 px-2 rounded">
              <span className="font-medium">RECEITA LÍQUIDA</span>
              <span className="font-mono font-medium">R$ {dreData.receitaLiquida.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</span>
            </div>

            {/* CMV */}
            <div className="flex justify-between py-2 pl-4 text-muted-foreground pt-4">
              <span>(-) Custo da Mercadoria Vendida (CMV)</span>
              <span className="font-mono">R$ {dreData.custoMercadoria.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</span>
            </div>
            <div className="flex justify-between py-2 pl-4 text-muted-foreground">
              <span>(-) Taxas de Marketplace</span>
              <span className="font-mono">R$ {dreData.taxasMarketplace.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</span>
            </div>
            <div className="flex justify-between py-3 border-b border-border bg-muted/50 px-2 rounded">
              <span className="font-medium">LUCRO BRUTO</span>
              <span className="font-mono font-medium">R$ {dreData.lucroBruto.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</span>
            </div>

            {/* Despesas Operacionais */}
            <div className="flex justify-between py-3 border-b border-border pt-4">
              <span className="font-medium">DESPESAS OPERACIONAIS</span>
              <span className="font-mono">R$ {dreData.despesasOperacionais.total.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</span>
            </div>
            <div className="flex justify-between py-2 pl-4 text-muted-foreground">
              <span>Marketing e Publicidade</span>
              <span className="font-mono">R$ {dreData.despesasOperacionais.marketing.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</span>
            </div>
            <div className="flex justify-between py-2 pl-4 text-muted-foreground">
              <span>Frete e Logística</span>
              <span className="font-mono">R$ {dreData.despesasOperacionais.frete.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</span>
            </div>
            <div className="flex justify-between py-2 pl-4 text-muted-foreground">
              <span>Embalagens</span>
              <span className="font-mono">R$ {dreData.despesasOperacionais.embalagens.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</span>
            </div>
            <div className="flex justify-between py-2 pl-4 text-muted-foreground">
              <span>Software e Ferramentas</span>
              <span className="font-mono">R$ {dreData.despesasOperacionais.software.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</span>
            </div>
            <div className="flex justify-between py-2 pl-4 text-muted-foreground">
              <span>Pessoal</span>
              <span className="font-mono">R$ {dreData.despesasOperacionais.funcionarios.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</span>
            </div>

            {/* Lucro Operacional */}
            <div className="flex justify-between py-3 border-b border-border bg-muted/50 px-2 rounded">
              <span className="font-medium">LUCRO OPERACIONAL (EBIT)</span>
              <span className="font-mono font-medium">R$ {dreData.lucroOperacional.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</span>
            </div>

            {/* Impostos */}
            <div className="flex justify-between py-2 pl-4 text-muted-foreground pt-4">
              <span>(-) Impostos (Simples Nacional)</span>
              <span className="font-mono">R$ {dreData.impostos.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</span>
            </div>

            {/* Lucro Líquido */}
            <div className="flex justify-between py-4 border-t-2 border-foreground/20 mt-4 bg-muted px-2 rounded">
              <span className="font-bold text-lg">LUCRO LÍQUIDO</span>
              <span className="font-mono font-bold text-lg">R$ {dreData.lucroLiquido.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</span>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Comparativo */}
      <Card>
        <CardHeader>
          <CardTitle>Comparativo Mensal</CardTitle>
          <CardDescription>Evolução dos últimos 6 meses</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {[
              { mes: "Jan/24", receita: 44640, lucro: 10692.80 },
              { mes: "Dez/23", receita: 52100, lucro: 12504.00 },
              { mes: "Nov/23", receita: 38900, lucro: 9336.00 },
              { mes: "Out/23", receita: 41200, lucro: 9888.00 },
              { mes: "Set/23", receita: 35600, lucro: 8544.00 },
              { mes: "Ago/23", receita: 33200, lucro: 7968.00 },
            ].map((item, index) => (
              <div key={item.mes} className="flex items-center gap-4">
                <div className="w-16 text-sm text-muted-foreground">{item.mes}</div>
                <div className="flex-1 flex gap-2">
                  <div className="flex-1">
                    <div className="h-6 bg-muted rounded overflow-hidden">
                      <div 
                        className="h-full bg-foreground/20 rounded" 
                        style={{ width: `${(item.receita / 55000) * 100}%` }}
                      />
                    </div>
                  </div>
                  <div className="w-24 text-right text-sm">
                    R$ {(item.receita / 1000).toFixed(1)}k
                  </div>
                </div>
              </div>
            ))}
          </div>
          <div className="flex gap-4 mt-4 pt-4 border-t border-border text-sm text-muted-foreground">
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 bg-foreground/20 rounded" />
              <span>Receita Líquida</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
