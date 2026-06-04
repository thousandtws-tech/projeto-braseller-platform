"use client"

import { useState } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

const mockContadores = [
  { 
    id: "CTD-001", 
    nome: "João Silva Contabilidade", 
    email: "joao@contabilidade.com", 
    telefone: "(11) 99999-0001",
    status: "ativo",
    ultimoAcesso: "2024-01-15T10:30:00",
    permissoes: ["visualizar_dre", "exportar_relatorios"]
  },
  { 
    id: "CTD-002", 
    nome: "Maria Santos Escritório", 
    email: "maria@escritorio.com", 
    telefone: "(11) 99999-0002",
    status: "pendente",
    ultimoAcesso: null,
    permissoes: ["visualizar_dre"]
  },
]

export default function ContadoresPage() {
  const [showModal, setShowModal] = useState(false)
  const [showPermissoes, setShowPermissoes] = useState<string | null>(null)

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Contadores</h1>
          <p className="text-muted-foreground">Gerencie o acesso de contadores aos seus dados fiscais</p>
        </div>
        <Button onClick={() => setShowModal(true)}>
          <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
          </svg>
          Convidar Contador
        </Button>
      </div>

      {/* Info */}
      <Card className="border-dashed">
        <CardContent className="pt-6">
          <div className="flex gap-4">
            <div className="w-10 h-10 rounded-full bg-muted border border-border flex items-center justify-center shrink-0">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <h3 className="font-medium">Acesso para Contadores</h3>
              <p className="text-sm text-muted-foreground mt-1">
                Convide seu contador para acessar os relatórios fiscais diretamente. 
                Eles terão acesso somente leitura aos dados que você autorizar, 
                facilitando a gestão contábil da sua empresa.
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Lista de Contadores */}
      <Card>
        <CardHeader>
          <CardTitle>Contadores com Acesso</CardTitle>
          <CardDescription>Gerencie quem pode visualizar seus dados fiscais</CardDescription>
        </CardHeader>
        <CardContent>
          {mockContadores.length > 0 ? (
            <div className="space-y-4">
              {mockContadores.map((contador) => (
                <div 
                  key={contador.id} 
                  className="flex flex-col md:flex-row md:items-center justify-between p-4 border border-border rounded-lg gap-4"
                >
                  <div className="flex items-center gap-4">
                    <div className="w-12 h-12 rounded-full bg-muted border border-border flex items-center justify-center">
                      <span className="text-lg font-medium">{contador.nome.charAt(0)}</span>
                    </div>
                    <div>
                      <h4 className="font-medium">{contador.nome}</h4>
                      <p className="text-sm text-muted-foreground">{contador.email}</p>
                      <p className="text-xs text-muted-foreground">{contador.telefone}</p>
                    </div>
                  </div>
                  <div className="flex flex-col md:flex-row items-start md:items-center gap-2 md:gap-4">
                    <div className="flex flex-col items-start md:items-end">
                      <span className={`inline-flex items-center px-2 py-1 rounded-md text-xs ${
                        contador.status === "ativo" ? "bg-muted" : "bg-muted border border-dashed border-border"
                      }`}>
                        {contador.status === "ativo" ? "Ativo" : "Convite Pendente"}
                      </span>
                      {contador.ultimoAcesso && (
                        <span className="text-xs text-muted-foreground mt-1">
                          Último acesso: {new Date(contador.ultimoAcesso).toLocaleDateString("pt-BR")}
                        </span>
                      )}
                    </div>
                    <div className="flex gap-2">
                      <Button 
                        variant="outline" 
                        size="sm"
                        onClick={() => setShowPermissoes(showPermissoes === contador.id ? null : contador.id)}
                      >
                        Permissões
                      </Button>
                      <Button variant="outline" size="sm" className="text-destructive">
                        Revogar
                      </Button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8">
              <div className="w-16 h-16 rounded-full bg-muted border border-border flex items-center justify-center mx-auto mb-4">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-muted-foreground" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
              </div>
              <h3 className="font-medium">Nenhum contador cadastrado</h3>
              <p className="text-sm text-muted-foreground mt-1">Convide seu contador para facilitar a gestão fiscal</p>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Permissões Expandidas */}
      {showPermissoes && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Permissões do Contador</CardTitle>
            <CardDescription>Defina quais dados o contador pode acessar</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {[
                { id: "visualizar_dre", nome: "Visualizar DRE", desc: "Acesso ao Demonstrativo de Resultado" },
                { id: "exportar_relatorios", nome: "Exportar Relatórios", desc: "Permite baixar relatórios em PDF/Excel" },
                { id: "visualizar_lancamentos", nome: "Visualizar Lançamentos", desc: "Acesso ao histórico de vendas" },
                { id: "visualizar_despesas", nome: "Visualizar Despesas", desc: "Acesso às despesas cadastradas" },
              ].map((perm) => (
                <div key={perm.id} className="flex items-center justify-between p-3 border border-border rounded-lg">
                  <div>
                    <h4 className="font-medium text-sm">{perm.nome}</h4>
                    <p className="text-xs text-muted-foreground">{perm.desc}</p>
                  </div>
                  <input 
                    type="checkbox" 
                    className="rounded border-input h-5 w-5" 
                    defaultChecked={mockContadores.find(c => c.id === showPermissoes)?.permissoes.includes(perm.id)}
                  />
                </div>
              ))}
              <Button className="w-full">Salvar Permissões</Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Modal Convite */}
      {showModal && (
        <div className="fixed inset-0 z-50 bg-foreground/20 flex items-center justify-center p-4">
          <Card className="w-full max-w-md">
            <CardHeader>
              <CardTitle>Convidar Contador</CardTitle>
              <CardDescription>Envie um convite por email para seu contador</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="nome">Nome do Escritório/Contador</Label>
                <Input id="nome" placeholder="Ex: João Silva Contabilidade" />
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input id="email" type="email" placeholder="contador@email.com" />
              </div>
              <div className="space-y-2">
                <Label htmlFor="telefone">Telefone (opcional)</Label>
                <Input id="telefone" placeholder="(11) 99999-0000" />
              </div>
              <div className="space-y-2">
                <Label>Permissões Iniciais</Label>
                <div className="space-y-2">
                  <label className="flex items-center gap-2">
                    <input type="checkbox" className="rounded border-input" defaultChecked />
                    <span className="text-sm">Visualizar DRE</span>
                  </label>
                  <label className="flex items-center gap-2">
                    <input type="checkbox" className="rounded border-input" />
                    <span className="text-sm">Exportar Relatórios</span>
                  </label>
                  <label className="flex items-center gap-2">
                    <input type="checkbox" className="rounded border-input" />
                    <span className="text-sm">Visualizar Lançamentos</span>
                  </label>
                </div>
              </div>
              <div className="flex gap-2 pt-4">
                <Button variant="outline" className="flex-1" onClick={() => setShowModal(false)}>
                  Cancelar
                </Button>
                <Button className="flex-1" onClick={() => setShowModal(false)}>
                  Enviar Convite
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  )
}
