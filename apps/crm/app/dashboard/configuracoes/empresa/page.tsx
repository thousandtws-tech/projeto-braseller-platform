"use client"

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

export default function ConfiguracoesEmpresaPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Configurações da Empresa</h1>
        <p className="text-muted-foreground">Gerencie os dados do seu tenant</p>
      </div>

      {/* Dados da Empresa */}
      <Card>
        <CardHeader>
          <CardTitle>Informações da Empresa</CardTitle>
          <CardDescription>Dados cadastrais da sua loja</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 md:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="razaoSocial">Razão Social</Label>
              <Input id="razaoSocial" defaultValue="Loja Exemplo LTDA" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="nomeFantasia">Nome Fantasia</Label>
              <Input id="nomeFantasia" defaultValue="Loja Exemplo" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="cnpj">CNPJ</Label>
              <Input id="cnpj" defaultValue="12.345.678/0001-90" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="ie">Inscrição Estadual</Label>
              <Input id="ie" defaultValue="123.456.789.012" />
            </div>
          </div>
          <div className="space-y-2">
            <Label htmlFor="endereco">Endereço</Label>
            <Input id="endereco" defaultValue="Rua Exemplo, 123 - Centro - São Paulo/SP" />
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="telefone">Telefone</Label>
              <Input id="telefone" defaultValue="(11) 3456-7890" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" defaultValue="contato@lojaexemplo.com.br" />
            </div>
          </div>
          <Button>Salvar Alterações</Button>
        </CardContent>
      </Card>

      {/* Configurações Fiscais */}
      <Card>
        <CardHeader>
          <CardTitle>Configurações Fiscais</CardTitle>
          <CardDescription>Regime tributário e configurações de impostos</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 md:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="regime">Regime Tributário</Label>
              <select 
                id="regime"
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                defaultValue="simples"
              >
                <option value="simples">Simples Nacional</option>
                <option value="presumido">Lucro Presumido</option>
                <option value="real">Lucro Real</option>
                <option value="mei">MEI</option>
              </select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="aliquota">Alíquota Simples (%)</Label>
              <Input id="aliquota" type="number" defaultValue="6.0" step="0.1" />
            </div>
          </div>
          <Button>Salvar Alterações</Button>
        </CardContent>
      </Card>

      {/* Usuários do Tenant */}
      <Card>
        <CardHeader>
          <CardTitle>Usuários</CardTitle>
          <CardDescription>Gerencie quem tem acesso ao sistema</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {[
              { nome: "João da Silva", email: "joao@lojaexemplo.com.br", role: "admin", status: "ativo" },
              { nome: "Maria Santos", email: "maria@lojaexemplo.com.br", role: "operador", status: "ativo" },
            ].map((user, index) => (
              <div key={index} className="flex items-center justify-between p-4 border border-border rounded-lg">
                <div className="flex items-center gap-4">
                  <div className="w-10 h-10 rounded-full bg-muted border border-border flex items-center justify-center">
                    <span className="font-medium">{user.nome.charAt(0)}</span>
                  </div>
                  <div>
                    <h4 className="font-medium">{user.nome}</h4>
                    <p className="text-sm text-muted-foreground">{user.email}</p>
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  <span className="inline-flex items-center px-2 py-1 rounded-md bg-muted text-xs">
                    {user.role === "admin" ? "Administrador" : "Operador"}
                  </span>
                  <Button variant="outline" size="sm">Editar</Button>
                </div>
              </div>
            ))}
            <Button variant="outline" className="w-full">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
              </svg>
              Adicionar Usuário
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Zona de Perigo */}
      <Card className="border-dashed">
        <CardHeader>
          <CardTitle>Zona de Perigo</CardTitle>
          <CardDescription>Ações irreversíveis para sua conta</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between p-4 border border-dashed border-border rounded-lg">
            <div>
              <h4 className="font-medium">Exportar Dados</h4>
              <p className="text-sm text-muted-foreground">Baixe todos os seus dados em formato JSON</p>
            </div>
            <Button variant="outline">Exportar</Button>
          </div>
          <div className="flex items-center justify-between p-4 border border-dashed border-border rounded-lg">
            <div>
              <h4 className="font-medium">Excluir Empresa</h4>
              <p className="text-sm text-muted-foreground">Remove permanentemente todos os dados</p>
            </div>
            <Button variant="outline" className="text-destructive">Excluir</Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
