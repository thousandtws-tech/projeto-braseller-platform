import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { DashboardStore } from '../state/dashboard.store';
import { OrdersTable } from '../components/orders-table';
import { CustomButton } from '../../../shared/components/custom-button';
import { SkeletonLoader } from '../../../shared/components/skeleton-loader';
import { EmptyState } from '../../../shared/components/empty-state';
import { Order } from '../../../core/models/user.model';

@Component({
  selector: 'app-dashboard-page',
  imports: [
    CommonModule, 
    DecimalPipe, 
    OrdersTable, 
    CustomButton, 
    SkeletonLoader, 
    EmptyState
  ],
  template: `
    <div class="space-y-6">
      
      <!-- HEADER DO DASHBOARD -->
      <header class="flex justify-between items-center bg-[#f7f7f7] border border-[#dee1e6] p-6 rounded-2xl">
        <div>
          <h2 class="text-lg font-bold font-display text-[#0a0b0d] tracking-tight">Estatísticas Consolidadas</h2>
          <p class="text-xs text-[#5b616e]">Dados carregados dos serviços conectados ao gateway.</p>
        </div>
        <app-custom-button
          text="Atualizar Dados"
          icon="sync"
          [loading]="store.loading()"
          (clicked)="store.loadDashboardData()"
        ></app-custom-button>
      </header>

      <!-- CARREGANDO ESTADOS DE ESQUELETO -->
      @if (store.loading()) {
        <div class="grid grid-cols-1 md:grid-cols-3 gap-5">
          <app-skeleton-loader mode="card"></app-skeleton-loader>
          <app-skeleton-loader mode="card"></app-skeleton-loader>
          <app-skeleton-loader mode="card"></app-skeleton-loader>
        </div>
        <app-skeleton-loader mode="table"></app-skeleton-loader>
      } @else {
        
        <!-- RESUMO DOS CARDS (DASHBOARD REAL) -->
        <section class="grid grid-cols-1 md:grid-cols-3 gap-5">
          <!-- Card 1: Faturamento Bruto -->
          <div class="p-5 border border-[#dee1e6] rounded-2xl bg-white shadow-sm">
            <span class="text-xs text-[#7c828a] font-medium uppercase block mb-1">Bruto Total</span>
            <p class="text-2xl font-mono font-medium text-[#0a0b0d]">
              R$ {{ store.grossValue() | number:'1.2-2':'pt-BR' }}
            </p>
            <span class="text-[10px] text-[#7c828a] mt-2 block">Aguardando histórico real</span>
          </div>

          <!-- Card 2: Lançamento Líquido -->
          <div class="p-5 border border-[#dee1e6] rounded-2xl bg-white shadow-sm">
            <span class="text-xs text-[#7c828a] font-medium uppercase block mb-1">Repasse Líquido</span>
            <p class="text-2xl font-mono font-medium text-[#05b169]">
              R$ {{ store.netValue() | number:'1.2-2':'pt-BR' }}
            </p>
            <span class="text-[10px] text-[#7c828a] mt-2 block">Calculado a partir dos lançamentos recebidos</span>
          </div>

          <!-- Card 3: Taxas Consolidadas -->
          <div class="p-5 border border-[#dee1e6] rounded-2xl bg-[#0a0b0d] text-white shadow-sm">
            <span class="text-[11px] text-[#a8acb3] uppercase block mb-1">Taxas Retidas</span>
            <p class="text-2xl font-mono font-medium text-white">
              R$ {{ store.platformFee() | number:'1.2-2':'pt-BR' }}
            </p>
            <div class="flex justify-between text-[10px] text-gray-400 mt-2">
              <span>Média Canal:</span>
              <span class="text-[#cf202f] font-bold">--</span>
            </div>
          </div>
        </section>

        <!-- SUBSECTION DE TABELAS DE SUCESSO -->
        @if (store.totalOrdersCount() > 0) {
          <div class="space-y-3">
            <h3 class="text-sm font-bold text-[#0a0b0d]">Histórico de Lançamentos</h3>
            <app-orders-table 
              [orders]="store.orders()"
              (viewDossier)="onViewDossier($event)"
            ></app-orders-table>
          </div>
        } @else {
          <!-- EMPTY STATE INTEGRADO -->
          <app-empty-state
            title="Nenhum Lançamento Encontrado"
            description="Sem vendas ativas conectadas via API. Conecte módulos de marketplace quando os endpoints estiverem disponíveis."
            icon="cloud_off"
            actionText="Atualizar dados"
            actionIcon="cloud_sync"
            (actionClicked)="store.loadDashboardData()"
          ></app-empty-state>
        }
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardPage implements OnInit {
  // Injeta o Store reativo com Signals (Angular 21 style)
  readonly store = inject(DashboardStore);

  ngOnInit(): void {
    // Carrega dados iniciais na montagem da tela
    this.store.loadDashboardData();
  }

  onViewDossier(order: Order): void {
    alert(`Visualizando Dossiê do Lançamento: ${order.id}\nComprador: ${order.buyerName}\nValor Bruto: R$ ${order.grossValue.toFixed(2)}`);
  }
}
