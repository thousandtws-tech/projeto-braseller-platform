import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { Order } from '../../../core/models/user.model';
import { TrackByUtils } from '../../../shared/utils/track-by.utils';

/**
 * Orders Table Presentation (Dumb/Dumb) Component
 * Focado estritamente em renderização, velocidade e reatividade baseada em inputs de Signals do componente pai.
 */
@Component({
  selector: 'app-orders-table',
  imports: [CommonModule, DecimalPipe, DatePipe],
  template: `
    <div class="border border-[#dee1e6] rounded-2xl overflow-hidden bg-white shadow-xs">
      <table class="w-full text-left font-sans text-xs">
        <thead class="bg-[#f7f7f7] border-b border-[#dee1e6]">
          <tr>
            <th scope="col" class="px-5 py-3 font-bold text-[#7c828a] uppercase tracking-wider">Identificador / Canal</th>
            <th scope="col" class="px-5 py-3 font-bold text-[#7c828a] uppercase tracking-wider">Lançamento / Pagamento</th>
            <th scope="col" class="px-5 py-3 font-bold text-[#7c828a] uppercase tracking-wider">Comprador</th>
            <th scope="col" class="px-5 py-3 font-bold text-[#7c828a] uppercase tracking-wider">Bruto (A)</th>
            <th scope="col" class="px-5 py-3 font-bold text-[#7c828a] uppercase tracking-wider">Líquido de Taxas</th>
            <th scope="col" class="px-5 py-3 font-bold text-[#7c828a] uppercase tracking-wider">Status Fiscal</th>
            <th scope="col" class="px-5 py-3 text-right">Dossiê</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-[#dee1e6]">
          @for (order of orders(); track orderTrackBy( $index, order )) {
            <tr class="hover:bg-[#f7f7f7]/50 transition-all">
              <td class="px-5 py-3.5">
                <div class="flex items-center gap-2">
                  <span class="w-2.5 h-2.5 rounded-full" 
                    [class.bg-[#ffe600]]="order.platform === 'ml'"
                    [class.bg-[#ee4d2d]]="order.platform === 'shopee'"
                    [class.bg-[#0052ff]]="order.platform === 'amazon'"
                    [class.bg-[#05b169]]="order.platform === 'manual'"
                  ></span>
                  <div>
                    <span class="font-mono font-bold text-gray-900 block leading-tight">{{ order.id }}</span>
                    <span class="text-[10px] text-[#7c828a] uppercase tracking-wider font-semibold">{{ order.platform.toUpperCase() }}</span>
                  </div>
                </div>
              </td>
              <td class="px-5 py-3.5">
                <span class="text-gray-900 block font-medium">{{ order.date | date:'dd MMM yyyy, HH:mm' }}</span>
                <span class="text-[10px] text-gray-400 leading-none">{{ order.paymentMethod }}</span>
              </td>
              <td class="px-5 py-3.5">
                <span class="font-semibold text-gray-900 block truncate max-w-[150px]">{{ order.buyerName }}</span>
                @if (order.invoiceNumber) {
                  <span class="text-[10px] text-gray-400 font-mono">{{ order.invoiceNumber }}</span>
                }
              </td>
              <td class="px-5 py-3.5 font-mono text-gray-900 font-semibold">
                R$ {{ order.grossValue | number:'1.2-2':'pt-BR' }}
              </td>
              <td class="px-5 py-3.5 font-mono font-medium text-[#05b169]">
                R$ {{ order.netValue | number:'1.2-2':'pt-BR' }}
              </td>
              <td class="px-5 py-3.5">
                <span class="px-2 py-0.5 text-[9px] font-bold tracking-wider rounded-full uppercase"
                  [class]="order.status === 'paid' ? 'bg-green-50 text-[#05b169] border border-green-200' :
                           order.status === 'pending' ? 'bg-amber-50 text-[#f4b000] border border-amber-200' :
                           'bg-red-50 text-[#cf202f] border border-red-200'"
                >
                  {{ order.status === 'paid' ? 'CONCLUÍDO' : order.status === 'pending' ? 'PENDENTE' : 'CANCELADO' }}
                </span>
              </td>
              <td class="px-5 py-3.5 text-right">
                <button 
                  [id]="'tbl-btn-' + order.id"
                  type="button"
                  (click)="viewDossier.emit(order)"
                  class="px-3 py-1 bg-white border border-[#dee1e6] hover:border-gray-400 text-[10px] font-bold rounded-full text-gray-700 transition-all cursor-pointer"
                >
                  Visualizar
                </button>
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="7" class="p-8 text-center text-xs text-gray-400 font-medium">
                Nenhum lançamento fiscal localizado para os filtros selecionados.
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OrdersTable {
  // Inputs definidos reativamente por Signals
  orders = input.required<Order[]>();

  // Eventos de Output reativo com nova API nativa
  viewDossier = output<Order>();

  // TrackBy compartilhado reutilizando o helper estático de performance DOM
  orderTrackBy = TrackByUtils.byId<Order>();
}
