"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import {
  LayoutDashboard,
  FileText,
  Receipt,
  TrendingUp,
  Plug,
  Settings,
  HelpCircle,
  Users,
  Bell,
  X,
} from "lucide-react"

const menuItems = [
  {
    title: "Financeiro",
    items: [
      { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
      { label: "Lançamentos", href: "/dashboard/lancamentos", icon: FileText },
      { label: "Despesas", href: "/dashboard/despesas", icon: Receipt },
      { label: "DRE", href: "/dashboard/dre", icon: TrendingUp },
    ],
  },
  {
    title: "Integrações",
    items: [
      { label: "Conectores", href: "/dashboard/conectores", icon: Plug },
    ],
  },
  {
    title: "Gestão",
    items: [
      { label: "Contadores", href: "/dashboard/contadores", icon: Users },
      { label: "Notificações", href: "/dashboard/notificacoes", icon: Bell },
    ],
  },
  {
    title: "Sistema",
    items: [
      { label: "Configurações", href: "/dashboard/configuracoes", icon: Settings },
      { label: "Ajuda", href: "/dashboard/ajuda", icon: HelpCircle },
    ],
  },
]

interface MobileSidebarProps {
  isOpen: boolean
  onClose: () => void
}

export function MobileSidebar({ isOpen, onClose }: MobileSidebarProps) {
  const pathname = usePathname()

  if (!isOpen) return null

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-foreground/20 z-40 lg:hidden"
        onClick={onClose}
      />

      {/* Sidebar */}
      <aside className="fixed inset-y-0 left-0 w-72 bg-card border-r border-border z-50 lg:hidden flex flex-col">
        {/* Header */}
        <div className="h-16 flex items-center justify-between px-6 border-b border-border">
          <Link href="/dashboard" className="flex items-center gap-3" onClick={onClose}>
            <div className="w-8 h-8 rounded-lg bg-muted border border-border flex items-center justify-center">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                className="w-5 h-5 text-foreground"
              >
                <path d="m8 3 4 8 5-5 5 15H2L8 3z" />
              </svg>
            </div>
            <span className="font-semibold text-foreground">Brasaller</span>
          </Link>
          <Button variant="ghost" size="icon" onClick={onClose}>
            <X className="w-5 h-5" />
            <span className="sr-only">Fechar menu</span>
          </Button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto py-4">
          {menuItems.map((group) => (
            <div key={group.title} className="px-3 mb-6">
              <p className="px-3 mb-2 text-xs font-medium uppercase tracking-wider text-muted-foreground">
                {group.title}
              </p>
              <ul className="space-y-1">
                {group.items.map((item) => {
                  const isActive = pathname === item.href
                  return (
                    <li key={item.href}>
                      <Link
                        href={item.href}
                        onClick={onClose}
                        className={cn(
                          "flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors",
                          isActive
                            ? "bg-muted text-foreground"
                            : "text-muted-foreground hover:bg-muted/50 hover:text-foreground"
                        )}
                      >
                        <item.icon className="w-5 h-5" />
                        {item.label}
                      </Link>
                    </li>
                  )
                })}
              </ul>
            </div>
          ))}
        </nav>

        {/* Footer - Tenant Info */}
        <div className="p-4 border-t border-border">
          <div className="flex items-center gap-3 px-3 py-2 rounded-md bg-muted/50">
            <div className="w-8 h-8 rounded-full bg-border flex items-center justify-center text-xs font-medium">
              ML
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-foreground truncate">Minha Loja</p>
              <p className="text-xs text-muted-foreground truncate">Trial - 14 dias</p>
            </div>
          </div>
        </div>
      </aside>
    </>
  )
}
