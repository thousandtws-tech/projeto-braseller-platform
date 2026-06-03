import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { DashboardShell } from '../dashboard-shell';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-expenses-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-6 animate-fade-in" id="expenses-view-content">
      <section class="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div class="p-5 border border-[#dee1e6] rounded-2xl bg-white shadow-xs">
          <span class="text-xs text-[#7c828a] font-medium uppercase">Total de despesas</span>
          <p class="text-[28px] font-mono text-[#cf202f] leading-none mt-2">
            R$ {{ shell.getExpensesSum() | number:'1.2-2':'pt-BR' }}
          </p>
        </div>
        <div class="p-5 border border-[#dee1e6] rounded-2xl bg-white shadow-xs">
          <span class="text-xs text-[#7c828a] font-medium uppercase">Lancamentos</span>
          <p class="text-[28px] font-mono text-[#0a0b0d] leading-none mt-2">
            {{ shell.expenses().length }}
          </p>
        </div>
        <div class="p-5 border border-[#dee1e6] rounded-2xl bg-[#0a0b0d] text-white shadow-xs">
          <span class="text-xs text-white/60 font-medium uppercase">Ticket medio</span>
          <p class="text-[28px] font-mono leading-none mt-2">
            R$ {{ (shell.expenses().length ? shell.getExpensesSum() / shell.expenses().length : 0) | number:'1.2-2':'pt-BR' }}
          </p>
        </div>
      </section>

      <section class="border border-[#dee1e6] rounded-2xl bg-white shadow-xs overflow-hidden">
        <div class="p-4 border-b border-[#dee1e6] flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <h3 class="text-sm font-bold text-[#0a0b0d]">Despesas operacionais</h3>
            <p class="text-[11px] text-[#5b616e]">Dados vindos do reporting-service.</p>
          </div>
          <div class="flex gap-2">
            <button
              type="button"
              (click)="shell.loadExpenses()"
              [disabled]="shell.isLoadingExpenses()"
              class="px-4 py-2 text-xs font-bold bg-white border border-[#dee1e6] rounded-full disabled:opacity-50"
            >
              Atualizar
            </button>
            <button
              type="button"
              (click)="shell.openCreateExpense()"
              class="px-4 py-2 text-xs font-bold bg-[#0052ff] text-white rounded-full"
            >
              Nova despesa
            </button>
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-left text-xs">
            <thead class="bg-[#f7f7f7] border-b border-[#dee1e6]">
              <tr>
                <th class="px-5 py-3 uppercase text-[#7c828a]">Data</th>
                <th class="px-5 py-3 uppercase text-[#7c828a]">Categoria</th>
                <th class="px-5 py-3 uppercase text-[#7c828a]">Descricao</th>
                <th class="px-5 py-3 uppercase text-[#7c828a]">Valor</th>
                <th class="px-5 py-3 text-right">Acoes</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[#dee1e6]">
              @if (shell.isLoadingExpenses()) {
                <tr><td colspan="5" class="px-5 py-12 text-center text-[#7c828a]">Carregando despesas...</td></tr>
              }

              @for (expense of shell.expenses(); track expense.id) {
                <tr class="hover:bg-[#f7f7f7]/60">
                  <td class="px-5 py-3.5">{{ expense.expense_date | date:'dd/MM/yyyy' }}</td>
                  <td class="px-5 py-3.5 font-bold">{{ shell.getExpenseCategoryLabel(expense.category) }}</td>
                  <td class="px-5 py-3.5">{{ expense.description }}</td>
                  <td class="px-5 py-3.5 font-mono text-[#cf202f]">R$ {{ expense.amount | number:'1.2-2':'pt-BR' }}</td>
                  <td class="px-5 py-3.5">
                    <div class="flex justify-end gap-2">
                      <button
                        type="button"
                        (click)="shell.openEditExpense(expense)"
                        class="px-3 py-1 bg-white border border-[#dee1e6] rounded-full font-bold"
                      >
                        Editar
                      </button>
                      <button
                        type="button"
                        (click)="shell.deleteExpense(expense.id)"
                        class="px-3 py-1 bg-[#fff1f2] text-[#cf202f] border border-[#ffd7dc] rounded-full font-bold"
                      >
                        Excluir
                      </button>
                    </div>
                  </td>
                </tr>
              } @empty {
                @if (!shell.isLoadingExpenses()) {
                  <tr><td colspan="5" class="px-5 py-12 text-center text-[#7c828a]">Nenhuma despesa registrada.</td></tr>
                }
              }
            </tbody>
          </table>
        </div>
      </section>
    </div>
  `,
})
export class ExpensesPage implements OnInit {
  readonly shell = inject(DashboardShell);

  ngOnInit(): void {
    this.shell.loadExpenses();
  }
}
