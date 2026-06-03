import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found-page',
  imports: [RouterLink],
  template: `
    <div class="min-h-screen w-full flex flex-col items-center justify-center p-6 bg-white text-center">
      <div class="w-16 h-16 rounded-full bg-[#f7f7f7] text-[#cf202f] flex items-center justify-center mb-4">
        <span class="material-icons text-3xl">error_outline</span>
      </div>
      <h2 class="text-xl font-bold font-display text-[#0a0b0d] tracking-tight leading-none mb-2">Página Não Localizada</h2>
      <p class="text-xs text-[#5b616e] max-w-[280px] mb-6 leading-normal">
        O endereço digitado não existe em nossa arquitetura de rotas ou foi removido definitivamente.
      </p>
      <a
        routerLink="/dashboard"
        class="inline-flex items-center gap-1.5 px-5 py-2 bg-[#0052ff] hover:bg-[#003ecc] text-white text-xs font-bold rounded-full transition-colors"
      >
        Retornar ao Dashboard
      </a>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotFoundPage {}
