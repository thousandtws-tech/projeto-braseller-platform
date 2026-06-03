import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { DashboardShell } from '../dashboard-shell';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-connectors-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-6 animate-fade-in" id="connectors-view-content">
      <section class="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div class="p-5 border border-[#dee1e6] rounded-2xl bg-white shadow-xs">
          <span class="text-xs text-[#7c828a] font-medium uppercase">Plataformas</span>
          <p class="text-[28px] font-mono text-[#0a0b0d] leading-none mt-2">{{ shell.connectors().length }}</p>
        </div>
        <div class="p-5 border border-[#dee1e6] rounded-2xl bg-white shadow-xs">
          <span class="text-xs text-[#7c828a] font-medium uppercase">Ativas</span>
          <p class="text-[28px] font-mono text-[#05b169] leading-none mt-2">
            {{ getActiveConnectorsCount() }}
          </p>
        </div>
        <div class="p-5 border border-[#dee1e6] rounded-2xl bg-[#0a0b0d] text-white shadow-xs">
          <span class="text-xs text-white/60 font-medium uppercase">Core-service</span>
          <p class="text-[18px] font-bold leading-none mt-3">
            {{ shell.isLoadingConnectors() ? 'Sincronizando' : 'Conectado' }}
          </p>
        </div>
      </section>

      <section class="grid grid-cols-1 xl:grid-cols-2 gap-5">
        @if (shell.isLoadingConnectors()) {
          <div class="xl:col-span-2 p-12 border border-dashed border-[#dee1e6] rounded-2xl bg-white text-center text-xs text-[#7c828a]">
            Carregando conectores do core-service...
          </div>
        }

        @for (conn of shell.connectors(); track conn.key) {
          <article class="border border-[#dee1e6] rounded-2xl bg-white shadow-xs p-5 flex flex-col min-h-[320px]">
            <div class="flex items-start justify-between gap-4 mb-4">
              <div>
                <span
                  class="inline-flex items-center px-2 py-0.5 rounded-md text-[9px] font-bold uppercase tracking-wider mb-3"
                  [ngClass]="shell.getConnectorBadgeClass(conn)"
                >
                  {{ conn.type }}
                </span>
                <h3 class="text-lg font-bold text-[#0a0b0d]">{{ conn.name }}</h3>
                <p class="text-[11px] text-[#7c828a] font-mono mt-0.5">ID API: {{ conn.key }}</p>
              </div>
              <span class="w-3 h-3 rounded-full mt-1" [ngClass]="shell.getConnectorDotClass(conn)"></span>
            </div>

            <p class="text-xs text-[#5b616e] leading-relaxed mb-5">{{ conn.description }}</p>

            <div class="bg-[#f7f7f7] rounded-xl p-4 text-[11px] flex-grow">
              <div class="text-[10px] font-bold text-[#7c828a] uppercase mb-2">Contrato ativo</div>
              <ul class="space-y-1 font-mono text-[#0a0b0d]">
                @for (method of conn.requiredMethods; track method) {
                  <li>• {{ method }}</li>
                }
              </ul>

              @if (conn.optionalMethods.length > 0) {
                <div class="text-[10px] font-bold text-[#7c828a] uppercase mt-4 mb-2">Opcional</div>
                <ul class="space-y-1 font-mono text-[#5b616e]">
                  @for (method of conn.optionalMethods; track method) {
                    <li>• {{ method }}</li>
                  }
                </ul>
              }
            </div>

            <div class="mt-5 pt-4 border-t border-[#dee1e6] flex items-center justify-between gap-3">
              <div>
                <span class="block text-[10px] text-[#7c828a]">Status conexao</span>
                <span class="text-xs font-bold" [ngClass]="shell.getConnectorStatusClass(conn)">
                  {{ shell.getConnectorStatusLabel(conn) }}
                </span>
              </div>
              <button
                type="button"
                [disabled]="shell.tenant()?.role === 'accountant'"
                (click)="shell.toggleConnector(conn.key, conn.active)"
                class="px-4 py-2 text-xs font-bold rounded-full bg-[#0052ff] text-white disabled:opacity-50"
              >
                {{ shell.getConnectorActionLabel(conn) }}
              </button>
            </div>

            <p class="text-[10px] text-[#7c828a] mt-3">{{ shell.getConnectorStatusHint(conn) }}</p>
          </article>
        } @empty {
          @if (!shell.isLoadingConnectors()) {
            <div class="xl:col-span-2 p-12 border border-dashed border-[#dee1e6] rounded-2xl bg-white text-center">
              <span class="material-icons text-4xl text-[#7c828a] mb-3">extension_off</span>
              <h3 class="text-sm font-bold text-[#0a0b0d]">Nenhuma plataforma conectavel encontrada</h3>
              <p class="text-xs text-[#5b616e] mt-1">O core-service nao retornou conectores disponiveis.</p>
            </div>
          }
        }
      </section>
    </div>
  `,
})
export class ConnectorsPage {
  readonly shell = inject(DashboardShell);

  getActiveConnectorsCount(): number {
    return this.shell.connectors().filter(connector => connector.active).length;
  }
}
