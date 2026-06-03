import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { DashboardShell } from '../dashboard-shell';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-dre-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="space-y-6 animate-fade-in" id="dre-view-content">
      <section class="bg-[#f7f7f7] border border-[#dee1e6] p-4 rounded-2xl shadow-xs">
        <form [formGroup]="shell.dreForm" class="flex flex-col lg:flex-row lg:items-end justify-between gap-4">
          <div>
            <h3 class="text-sm font-bold text-[#0a0b0d]">DRE por periodo</h3>
            <p class="text-[11px] text-[#5b616e]">Consulta e enfileira calculos no reporting-service.</p>
          </div>

          <div class="grid grid-cols-2 md:flex items-end gap-3">
            <label class="block">
              <span class="block text-[10px] font-bold text-[#7c828a] uppercase mb-1">Inicio</span>
              <input type="date" formControlName="from" class="w-full md:w-[150px] p-2 text-xs border border-[#dee1e6] rounded-xl bg-white">
            </label>
            <label class="block">
              <span class="block text-[10px] font-bold text-[#7c828a] uppercase mb-1">Fim</span>
              <input type="date" formControlName="to" class="w-full md:w-[150px] p-2 text-xs border border-[#dee1e6] rounded-xl bg-white">
            </label>
          </div>

          <div class="flex gap-2">
            <button
              type="button"
              (click)="shell.loadDre()"
              [disabled]="shell.isLoadingDre() || shell.dreForm.invalid"
              class="px-4 py-2 text-xs font-bold bg-white border border-[#dee1e6] rounded-full disabled:opacity-50"
            >
              Consultar
            </button>
            <button
              type="button"
              (click)="shell.enqueueDre()"
              [disabled]="shell.isLoadingDre() || shell.dreForm.invalid"
              class="px-4 py-2 text-xs font-bold bg-[#0052ff] text-white rounded-full disabled:opacity-50"
            >
              Calcular
            </button>
          </div>
        </form>
      </section>

      @if (shell.dreJob()) {
        <section class="p-4 border border-[#dee1e6] rounded-2xl bg-white shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-3">
          <div>
            <span class="text-[10px] font-bold text-[#7c828a] uppercase">Job de calculo</span>
            <p class="text-sm font-mono text-[#0a0b0d]">{{ shell.dreJob()?.job_id }}</p>
          </div>
          <span class="px-3 py-1 rounded-full bg-[#eef0f3] text-xs font-bold text-[#5b616e]">
            {{ shell.getDreStatusLabel(shell.dreJob()?.status || '') }}
          </span>
        </section>
      }

      @if (shell.dreStatement()) {
        <section class="grid grid-cols-1 md:grid-cols-4 gap-5">
          <div class="p-5 border border-[#dee1e6] rounded-2xl bg-white shadow-xs">
            <span class="text-xs text-[#7c828a] font-medium uppercase">Receita bruta</span>
            <p class="text-[24px] font-mono text-[#0a0b0d] leading-none mt-2">
              R$ {{ shell.dreStatement()?.gross_revenue | number:'1.2-2':'pt-BR' }}
            </p>
          </div>
          <div class="p-5 border border-[#dee1e6] rounded-2xl bg-white shadow-xs">
            <span class="text-xs text-[#7c828a] font-medium uppercase">Taxas marketplace</span>
            <p class="text-[24px] font-mono text-[#cf202f] leading-none mt-2">
              R$ {{ shell.dreStatement()?.marketplace_fees | number:'1.2-2':'pt-BR' }}
            </p>
          </div>
          <div class="p-5 border border-[#dee1e6] rounded-2xl bg-white shadow-xs">
            <span class="text-xs text-[#7c828a] font-medium uppercase">Despesas</span>
            <p class="text-[24px] font-mono text-[#cf202f] leading-none mt-2">
              R$ {{ shell.dreStatement()?.operating_expenses | number:'1.2-2':'pt-BR' }}
            </p>
          </div>
          <div class="p-5 border border-[#dee1e6] rounded-2xl bg-[#0a0b0d] text-white shadow-xs">
            <span class="text-xs text-white/60 font-medium uppercase">Resultado liquido</span>
            <p class="text-[24px] font-mono leading-none mt-2">
              R$ {{ shell.dreStatement()?.net_result | number:'1.2-2':'pt-BR' }}
            </p>
          </div>
        </section>

        <section class="grid grid-cols-1 lg:grid-cols-2 gap-5">
          <div class="p-5 border border-[#dee1e6] rounded-2xl bg-white shadow-xs">
            <h3 class="text-sm font-bold text-[#0a0b0d] mb-4">Resumo fiscal</h3>
            <dl class="space-y-3 text-xs">
              <div class="flex justify-between">
                <dt class="text-[#7c828a]">Regime</dt>
                <dd class="font-bold">{{ shell.getDreTaxRegimeLabel(shell.dreStatement()?.tax_regime || '') }}</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-[#7c828a]">Aliquota estimada</dt>
                <dd class="font-mono">{{ shell.dreStatement()?.estimated_tax_rate | number:'1.2-2':'pt-BR' }}%</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-[#7c828a]">Impostos estimados</dt>
                <dd class="font-mono text-[#cf202f]">R$ {{ shell.dreStatement()?.estimated_taxes | number:'1.2-2':'pt-BR' }}</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-[#7c828a]">Vendas</dt>
                <dd class="font-mono">{{ shell.dreStatement()?.sales_count }}</dd>
              </div>
            </dl>
          </div>

          <div class="p-5 border border-[#dee1e6] rounded-2xl bg-white shadow-xs">
            <h3 class="text-sm font-bold text-[#0a0b0d] mb-4">Despesas por categoria</h3>
            <div class="space-y-3">
              @for (category of shell.dreStatement()?.expenses_by_category || []; track category.category) {
                <div>
                  <div class="flex justify-between text-xs mb-1">
                    <span class="font-bold">{{ shell.getExpenseCategoryLabel(category.category) }}</span>
                    <span class="font-mono">R$ {{ category.amount | number:'1.2-2':'pt-BR' }}</span>
                  </div>
                  <div class="h-2 bg-[#eef0f3] rounded-full overflow-hidden">
                    <div class="h-full bg-[#0052ff]" [style.width.%]="shell.dreStatement()?.operating_expenses ? (category.amount / shell.dreStatement()!.operating_expenses) * 100 : 0"></div>
                  </div>
                </div>
              } @empty {
                <p class="text-xs text-[#7c828a]">Sem despesas no periodo selecionado.</p>
              }
            </div>
          </div>
        </section>
      } @else {
        <section class="p-12 border border-dashed border-[#dee1e6] rounded-2xl bg-white text-center">
          <span class="material-icons text-4xl text-[#7c828a] mb-3">analytics</span>
          <h3 class="text-sm font-bold text-[#0a0b0d]">Nenhum DRE carregado</h3>
          <p class="text-xs text-[#5b616e] mt-1">Escolha o periodo e consulte ou calcule o demonstrativo.</p>
        </section>
      }
    </div>
  `,
})
export class DrePage {
  readonly shell = inject(DashboardShell);
}
