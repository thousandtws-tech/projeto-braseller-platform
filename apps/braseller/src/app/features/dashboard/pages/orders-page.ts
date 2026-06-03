import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { DashboardShell } from '../dashboard-shell';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-orders-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="space-y-6 animate-fade-in" id="orders-view-content">
      <div class="bg-[#f7f7f7] border border-[#dee1e6] p-4 rounded-2xl shadow-xs">
        <form [formGroup]="shell.filterForm" class="flex flex-col md:flex-row items-end justify-between gap-4">
          <div class="grid grid-cols-2 md:flex items-end gap-3 w-full md:w-auto">
            <label class="block">
              <span class="block text-[10px] font-bold text-[#7c828a] uppercase mb-1">Canal</span>
              <select formControlName="platform" class="w-full md:w-[150px] p-2 text-xs border border-[#dee1e6] rounded-xl bg-white">
                <option value="all">Todos</option>
                @for (option of shell.getPlatformFilterOptions(); track option.key) {
                  <option [value]="option.key">{{ option.name }}</option>
                }
              </select>
            </label>
            <label class="block">
              <span class="block text-[10px] font-bold text-[#7c828a] uppercase mb-1">Status</span>
              <select formControlName="status" class="w-full md:w-[150px] p-2 text-xs border border-[#dee1e6] rounded-xl bg-white">
                <option value="all">Todos</option>
                @for (status of shell.getStatusFilterOptions(); track status) {
                  <option [value]="status">{{ shell.getStatusFilterLabel(status) }}</option>
                }
              </select>
            </label>
          </div>

          <label class="block w-full md:w-[350px]">
            <span class="block text-[10px] font-bold text-[#7c828a] uppercase mb-1">Pesquisa</span>
            <input type="text" formControlName="search" placeholder="Pedido, comprador, NF..."
              class="w-full p-2 text-xs border border-[#dee1e6] rounded-xl bg-white">
          </label>

          <div class="flex gap-2 w-full md:w-auto">
            <button type="button" (click)="shell.clearFilters()" class="px-4 py-2 text-xs font-bold bg-white border border-[#dee1e6] rounded-full">Limpar</button>
            <button type="button" (click)="shell.openManualOrderModal()" class="px-4 py-2 text-xs font-bold bg-[#05b169] text-white rounded-full">Manual</button>
          </div>
        </form>
      </div>

      <div class="border border-[#dee1e6] rounded-2xl overflow-hidden bg-white shadow-xs">
        <table class="w-full text-left text-xs">
          <thead class="bg-[#f7f7f7] border-b border-[#dee1e6]">
            <tr>
              <th class="px-5 py-3 uppercase text-[#7c828a]">Pedido / Canal</th>
              <th class="px-5 py-3 uppercase text-[#7c828a]">Data</th>
              <th class="px-5 py-3 uppercase text-[#7c828a]">Comprador</th>
              <th class="px-5 py-3 uppercase text-[#7c828a]">Bruto</th>
              <th class="px-5 py-3 uppercase text-[#7c828a]">Liquido</th>
              <th class="px-5 py-3 uppercase text-[#7c828a]">Status</th>
              <th class="px-5 py-3 text-right">Acao</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-[#dee1e6]">
            @if (shell.isLoadingOrders()) {
              <tr><td colspan="7" class="px-5 py-12 text-center text-[#7c828a]">Carregando lancamentos...</td></tr>
            }
            @for (order of shell.orders(); track order.order_id) {
              <tr class="hover:bg-[#f7f7f7]/60">
                <td class="px-5 py-3.5">
                  <span class="w-2.5 h-2.5 rounded-full inline-block mr-2" [ngClass]="shell.getPlatformColorClass(order.platform)"></span>
                  <span class="font-mono font-bold">{{ order.order_id }}</span>
                  <span class="block ml-5 text-[10px] text-[#7c828a] uppercase">{{ shell.getPlatformName(order.platform) }}</span>
                </td>
                <td class="px-5 py-3.5">{{ order.date | date:'dd/MM/yyyy' }}</td>
                <td class="px-5 py-3.5">{{ order.buyer_name || '-' }}</td>
                <td class="px-5 py-3.5 font-mono">R$ {{ order.gross_value | number:'1.2-2' }}</td>
                <td class="px-5 py-3.5 font-mono text-[#05b169]">R$ {{ order.net_value | number:'1.2-2' }}</td>
                <td class="px-5 py-3.5 uppercase">{{ order.status }}</td>
                <td class="px-5 py-3.5 text-right">
                  <button type="button" (click)="shell.openOrderDetails(order)" class="px-3 py-1 bg-white border border-[#dee1e6] rounded-full font-bold">Dossie</button>
                </td>
              </tr>
            } @empty {
              @if (!shell.isLoadingOrders()) {
                <tr><td colspan="7" class="px-5 py-12 text-center text-[#7c828a]">Nenhum lancamento encontrado.</td></tr>
              }
            }
          </tbody>
        </table>
      </div>
    </div>
  `,
})
export class OrdersPage {
  readonly shell = inject(DashboardShell);
}
