import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, Router, RouterLinkActive } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

/**
 * Admin Layout (Template de Estruturação Administrativa)
 * Carrega componentes persistentes como Sidebar, Header de Contexto, Barramento de Notificações,
 * e distribui o conteúdo dinâmico utilizando a diretiva RouterOutlet do Angular Router.
 */
@Component({
  selector: 'app-admin-layout',
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, MatIconModule],
  template: `
    <div class="flex h-screen w-full bg-white text-[#0a0b0d] overflow-hidden font-sans">
      
      <!-- BARRA LATERAL (SIDEBAR PERSISTENTE) -->
      <aside class="w-[260px] border-r border-[#dee1e6] flex flex-col h-full bg-[#f7f7f7] shrink-0">
        <!-- Logo / Marca -->
        <div class="p-6 flex items-center gap-3">
          <div class="w-8 h-8 bg-[#0052ff] rounded-full flex items-center justify-center shadow-sm">
            <div class="w-4 h-4 bg-white rounded-sm rotate-[45deg]"></div>
          </div>
          <div>
            <span class="text-xl font-bold tracking-tight text-[#0a0b0d] block leading-none font-display">BraSeller</span>
            <span class="text-[10px] font-bold text-[#7c828a] uppercase tracking-wider">Enterprise Core</span>
          </div>
        </div>

        <!-- Tenant / Usuário Menu -->
        <div class="px-4 mb-4">
          <div class="bg-white border border-[#dee1e6] p-3 rounded-xl shadow-xs">
            <div class="flex items-center gap-2 mb-1">
              <mat-icon class="text-xs text-[#0052ff]">store</mat-icon>
              <span class="text-[11px] font-semibold text-[#5b616e] truncate">Tenant autenticado</span>
            </div>
            <div class="text-[9px] text-[#7c828a] font-mono">Contexto do gateway</div>
          </div>
        </div>

        <!-- Navegação Modular -->
        <nav class="flex-grow px-3 space-y-1 overflow-y-auto">
          <div class="px-3 py-2 text-[10px] font-bold uppercase tracking-widest text-[#7c828a]">Navegação Principal</div>
          
          <button
            type="button"
            routerLink="/admin/dashboard"
            routerLinkActive="bg-white border-[#dee1e6] text-[#0052ff] font-semibold shadow-xs"
            [routerLinkActiveOptions]="{ exact: true }"
            class="w-full flex items-center gap-3 px-3 py-2 border border-transparent rounded-full text-[#5b616e] hover:bg-white text-left transition-all cursor-pointer"
          >
            <mat-icon class="text-lg">dashboard</mat-icon>
            <span class="text-xs">Painel Consolidado</span>
          </button>
        </nav>

        <!-- Sair do Sistema -->
        <div class="p-4 border-t border-[#dee1e6]">
          <button
            type="button"
            (click)="onLogout()"
            class="w-full py-1.5 px-3 bg-[#cf202f]/10 text-[#cf202f] hover:bg-[#cf202f]/20 transition-all rounded-full font-bold text-xs flex items-center justify-center gap-1.5 cursor-pointer"
          >
            <mat-icon class="text-sm">logout</mat-icon>
            <span>Sair do Painel</span>
          </button>
        </div>
      </aside>

      <!-- PORTAL PRINCIPAL / CONTEÚDO ROTATIVO -->
      <div class="flex-grow flex flex-col h-full bg-white overflow-hidden">
        <!-- HEADER ACESSÍVEL -->
        <header class="h-[74px] border-b border-[#dee1e6] flex items-center justify-between px-8 bg-white shrink-0">
          <div>
            <span class="text-[10px] font-bold text-[#7c828a] uppercase tracking-widest">Workspace Corporativo</span>
            <h1 class="text-base font-bold text-[#0a0b0d] flex items-center gap-2 font-display">
              Gerenciador Central de Vendas
            </h1>
          </div>
          <div class="flex items-center gap-3">
            <span class="text-xs font-semibold px-2 py-0.5 rounded bg-gray-100 text-gray-700">Canal Seguro</span>
          </div>
        </header>

        <!-- PORT VIEW SECTOR COM ROTEAMENTO DINÂMICO -->
        <main class="flex-grow overflow-y-auto p-6 md:p-8">
          <router-outlet></router-outlet>
        </main>
      </div>

    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminLayout {
  private router = inject(Router);

  onLogout(): void {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('braseller_access_token');
      localStorage.removeItem('braseller_refresh_token');
      localStorage.removeItem('braseller_tenant');
    }
    this.router.navigate(['/login']);
  }
}
