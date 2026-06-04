"use client"

import { useState } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"

const mockNotificacoes = [
  { 
    id: "NOT-001", 
    tipo: "venda", 
    titulo: "Nova venda realizada",
    mensagem: "Venda de R$ 299,90 no Mercado Livre - Fone Bluetooth JBL",
    data: "2024-01-15T14:30:00",
    lida: false
  },
  { 
    id: "NOT-002", 
    tipo: "pagamento", 
    titulo: "Pagamento liberado",
    mensagem: "O pagamento de R$ 1.709,91 do Mercado Livre foi liberado para saque",
    data: "2024-01-15T12:00:00",
    lida: false
  },
  { 
    id: "NOT-003", 
    tipo: "alerta", 
    titulo: "Atenção: Token expirando",
    mensagem: "O token de acesso do Magazine Luiza irá expirar em 3 dias. Reconecte sua conta.",
    data: "2024-01-15T09:00:00",
    lida: false
  },
  { 
    id: "NOT-004", 
    tipo: "sistema", 
    titulo: "Sincronização concluída",
    mensagem: "156 vendas importadas do Mercado Livre com sucesso",
    data: "2024-01-14T18:00:00",
    lida: true
  },
  { 
    id: "NOT-005", 
    tipo: "venda", 
    titulo: "Nova venda realizada",
    mensagem: "Venda de R$ 3.499,00 na Amazon - Notebook Dell Inspiron",
    data: "2024-01-14T15:45:00",
    lida: true
  },
  { 
    id: "NOT-006", 
    tipo: "contador", 
    titulo: "Acesso de contador",
    mensagem: "João Silva Contabilidade acessou o relatório DRE de Janeiro",
    data: "2024-01-14T10:30:00",
    lida: true
  },
  { 
    id: "NOT-007", 
    tipo: "estorno", 
    titulo: "Estorno processado",
    mensagem: "Estorno de R$ 49,90 na Shopee - Devolução de Capa Celular",
    data: "2024-01-13T16:00:00",
    lida: true
  },
]

const tipoIcone = {
  venda: (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
  ),
  pagamento: (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z" />
    </svg>
  ),
  alerta: (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
    </svg>
  ),
  sistema: (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
    </svg>
  ),
  contador: (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
    </svg>
  ),
  estorno: (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" />
    </svg>
  ),
}

export default function NotificacoesPage() {
  const [notificacoes, setNotificacoes] = useState(mockNotificacoes)
  const [filtro, setFiltro] = useState("todas")

  const naoLidas = notificacoes.filter(n => !n.lida).length

  const marcarTodasComoLidas = () => {
    setNotificacoes(notificacoes.map(n => ({ ...n, lida: true })))
  }

  const marcarComoLida = (id: string) => {
    setNotificacoes(notificacoes.map(n => n.id === id ? { ...n, lida: true } : n))
  }

  const notificacoesFiltradas = filtro === "todas" 
    ? notificacoes 
    : filtro === "nao-lidas"
    ? notificacoes.filter(n => !n.lida)
    : notificacoes.filter(n => n.tipo === filtro)

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Notificações</h1>
          <p className="text-muted-foreground">
            {naoLidas > 0 ? `${naoLidas} notificações não lidas` : "Todas as notificações lidas"}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={marcarTodasComoLidas} disabled={naoLidas === 0}>
            <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
            Marcar todas como lidas
          </Button>
        </div>
      </div>

      {/* Filtros */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-wrap gap-2">
            {[
              { id: "todas", label: "Todas" },
              { id: "nao-lidas", label: "Não lidas" },
              { id: "venda", label: "Vendas" },
              { id: "pagamento", label: "Pagamentos" },
              { id: "alerta", label: "Alertas" },
              { id: "sistema", label: "Sistema" },
            ].map((f) => (
              <Button 
                key={f.id} 
                variant={filtro === f.id ? "default" : "outline"} 
                size="sm"
                onClick={() => setFiltro(f.id)}
              >
                {f.label}
                {f.id === "nao-lidas" && naoLidas > 0 && (
                  <span className="ml-2 bg-background text-foreground rounded-full px-2 py-0.5 text-xs">
                    {naoLidas}
                  </span>
                )}
              </Button>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Lista de Notificações */}
      <Card>
        <CardHeader>
          <CardTitle>Histórico</CardTitle>
          <CardDescription>Suas notificações dos últimos 30 dias</CardDescription>
        </CardHeader>
        <CardContent>
          {notificacoesFiltradas.length > 0 ? (
            <div className="space-y-1">
              {notificacoesFiltradas.map((notif) => (
                <div 
                  key={notif.id}
                  className={`flex items-start gap-4 p-4 rounded-lg hover:bg-muted/50 cursor-pointer transition-colors ${
                    !notif.lida ? "bg-muted/30" : ""
                  }`}
                  onClick={() => marcarComoLida(notif.id)}
                >
                  <div className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 ${
                    !notif.lida ? "bg-muted border-2 border-foreground/20" : "bg-muted border border-border"
                  }`}>
                    {tipoIcone[notif.tipo as keyof typeof tipoIcone]}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <h4 className={`font-medium text-sm ${!notif.lida ? "" : "text-muted-foreground"}`}>
                          {notif.titulo}
                        </h4>
                        <p className="text-sm text-muted-foreground mt-0.5">{notif.mensagem}</p>
                      </div>
                      {!notif.lida && (
                        <div className="w-2 h-2 rounded-full bg-foreground shrink-0 mt-2" />
                      )}
                    </div>
                    <p className="text-xs text-muted-foreground mt-2">
                      {new Date(notif.data).toLocaleString("pt-BR")}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8">
              <div className="w-16 h-16 rounded-full bg-muted border border-border flex items-center justify-center mx-auto mb-4">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-muted-foreground" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                </svg>
              </div>
              <h3 className="font-medium">Nenhuma notificação</h3>
              <p className="text-sm text-muted-foreground mt-1">Você não tem notificações nesta categoria</p>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Configurações de Notificação */}
      <Card>
        <CardHeader>
          <CardTitle>Preferências de Notificação</CardTitle>
          <CardDescription>Configure quais notificações deseja receber</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {[
              { id: "vendas", nome: "Novas Vendas", desc: "Receba uma notificação para cada nova venda", ativo: true },
              { id: "pagamentos", nome: "Pagamentos Liberados", desc: "Aviso quando pagamentos forem liberados para saque", ativo: true },
              { id: "alertas", nome: "Alertas do Sistema", desc: "Avisos sobre tokens expirando, erros de sync, etc.", ativo: true },
              { id: "contadores", nome: "Acesso de Contadores", desc: "Notificação quando um contador acessar seus dados", ativo: false },
              { id: "email", nome: "Resumo por Email", desc: "Receba um resumo diário por email", ativo: false },
            ].map((pref) => (
              <div key={pref.id} className="flex items-center justify-between p-3 border border-border rounded-lg">
                <div>
                  <h4 className="font-medium text-sm">{pref.nome}</h4>
                  <p className="text-xs text-muted-foreground">{pref.desc}</p>
                </div>
                <input 
                  type="checkbox" 
                  className="rounded border-input h-5 w-5" 
                  defaultChecked={pref.ativo}
                />
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
