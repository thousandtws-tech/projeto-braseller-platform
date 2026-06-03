import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { DashboardShell } from '../dashboard-shell';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-dashboard-home-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-6 animate-fade-in" id="dashboard-view-content">
      <section class="grid grid-cols-1 md:grid-cols-4 gap-5">
        <div class="p-5 border border-[#dee1e6] rounded-2xl bg-white shadow-xs">
          <span class="text-xs text-[#7c828a] font-medium uppercase">Faturamento</span>
          <p class="text-[28px] font-mono text-[#0a0b0d] leading-none mt-2">
            {{ (shell.summary()?.gross_value || 0) | number:'1.2-2':'pt-BR' }}
          </p>
        </div>
        <div class="p-5 border border-[#dee1e6] rounded-2xl bg-white shadow-xs">
          <span class="text-xs text-[#7c828a] font-medium uppercase">Recebido</span>
          <p class="text-[28px] font-mono text-[#05b169] leading-none mt-2">
            {{ (shell.summary()?.net_value || 0) | number:'1.2-2':'pt-BR' }}
          </p>
        </div>
        <div class="p-5 border border-[#dee1e6] rounded-2xl bg-[#0a0b0d] text-white shadow-xs">
          <span class="text-xs text-white/60 font-medium uppercase">Taxas</span>
          <p class="text-[28px] font-mono leading-none mt-2">
            {{ (shell.summary()?.platform_fee || 0) | number:'1.2-2':'pt-BR' }}
          </p>
        </div>
        <div class="p-5 border border-[#dee1e6] rounded-2xl bg-white shadow-xs">
          <span class="text-xs text-[#7c828a] font-medium uppercase">Lancamentos</span>
          <p class="text-[28px] font-mono text-[#0a0b0d] leading-none mt-2">
            {{ shell.summary()?.total_orders || 0 }}
          </p>
        </div>
      </section>

      <section class="p-5 border border-[#dee1e6] rounded-2xl bg-white shadow-xs">
        <div class="flex items-center justify-between gap-4 mb-4">
          <div>
            <h3 class="text-xs font-bold text-[#0a0b0d] uppercase tracking-wider">Split por plataforma</h3>
            <p class="text-[11px] text-[#5b616e]">Valores consolidados pelo reporting-service.</p>
          </div>
          <button type="button" (click)="shell.navigateTo('orders')" class="text-xs font-bold text-[#0052ff]">Ver lancamentos</button>
        </div>

        <div class="w-full h-4 bg-gray-100 rounded-full overflow-hidden flex">
          @for (entry of shell.getPlatformSplitEntries(); track entry.key) {
            <div class="h-full" [ngClass]="entry.colorClass" [style.width]="entry.width"></div>
          }
        </div>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
          @for (entry of shell.getPlatformSplitEntries(); track entry.key) {
            <div class="text-center">
              <span class="block text-[10px] text-gray-500 uppercase">{{ entry.name }}</span>
              <span class="text-sm font-semibold text-gray-900 font-mono">R$ {{ entry.value | number:'1.2-2' }}</span>
            </div>
          }
        </div>
      </section>

      @if (shell.monthlyEvolution().length > 0) {
        <section class="p-5 border border-[#dee1e6] rounded-2xl bg-white shadow-xs">
          <h3 class="text-xs font-bold text-[#0a0b0d] uppercase tracking-wider mb-4">Evolucao mensal</h3>
          <div class="flex items-end gap-2 h-28">
            @for (point of shell.monthlyEvolution(); track point.period) {
              <div class="flex-1 min-w-0 h-full flex flex-col justify-end gap-1">
                <div class="w-full rounded-t bg-[#0052ff]" [style.height.%]="(point.gross_value / shell.getMonthlyEvolutionMax()) * 100"></div>
                <span class="text-[9px] text-[#7c828a] font-mono truncate text-center">{{ point.period }}</span>
              </div>
            }
          </div>
        </section>
      }

      <section class="border border-[#dee1e6] rounded-2xl bg-white shadow-xs overflow-hidden">
        <div class="p-4 border-b border-[#dee1e6] flex items-center justify-between">
          <h3 class="text-sm font-bold text-[#0a0b0d]">Ultimos lancamentos</h3>
          <button type="button" (click)="shell.navigateTo('orders')" class="text-xs font-bold text-[#0052ff]">Abrir tabela</button>
        </div>
        <table class="w-full text-left text-xs">
          <tbody class="divide-y divide-[#dee1e6]">
            @for (order of shell.orders().slice(0, 5); track order.order_id) {
              <tr class="hover:bg-[#f7f7f7]">
                <td class="px-5 py-3.5 font-mono font-bold">{{ order.order_id }}</td>
                <td class="px-5 py-3.5">{{ shell.getPlatformName(order.platform) }}</td>
                <td class="px-5 py-3.5 font-mono">R$ {{ order.gross_value | number:'1.2-2' }}</td>
                <td class="px-5 py-3.5 text-right">
                  <button type="button" (click)="shell.openOrderDetails(order)" class="px-3 py-1 border border-[#dee1e6] rounded-full font-bold">Dossie</button>
                </td>
              </tr>
            } @empty {
              <tr><td class="px-5 py-12 text-center text-[#7c828a]">Nenhum lancamento encontrado.</td></tr>
            }
          </tbody>
        </table>
      </section>
    </div>
  `,
})
export class DashboardHomePage {
  readonly shell = inject(DashboardShell);
}
