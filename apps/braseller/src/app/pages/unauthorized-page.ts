import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-unauthorized-page',
  imports: [RouterLink],
  template: `
    <div class="min-h-screen w-full flex flex-col items-center justify-center p-6 bg-white text-center">
      <div class="w-16 h-16 rounded-full bg-red-50 text-[#cf202f] flex items-center justify-center mb-4">
        <span class="material-icons text-3xl">gavel</span>
      </div>
      <h2 class="text-xl font-bold font-display text-[#0a0b0d] tracking-tight leading-none mb-2">Acesso Não Autorizado</h2>
      <p class="text-xs text-[#5b616e] max-w-[320px] mb-6 leading-normal">
        Seu privilégio de perfil (role) ativo não possui permissões fiscais homologadas para conferência desta rota de auditoria.
      </p>
      <a
        routerLink="/dashboard"
        class="inline-flex items-center gap-1.5 px-5 py-2 bg-[#0a0b0d] hover:bg-[#1c1f24] text-white text-xs font-bold rounded-full transition-colors"
      >
        Retornar de Forma Segura
      </a>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UnauthorizedPage {}
