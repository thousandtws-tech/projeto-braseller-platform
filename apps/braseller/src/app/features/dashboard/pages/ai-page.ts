import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { DashboardShell } from '../dashboard-shell';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-ai-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="space-y-5 animate-fade-in h-full flex flex-col" id="chat-view-content">
      <section class="grid grid-cols-1 lg:grid-cols-[1fr_280px] gap-5 min-h-[520px]">
        <div class="border border-[#dee1e6] rounded-2xl bg-white shadow-xs flex flex-col overflow-hidden">
          <div class="p-4 border-b border-[#dee1e6] flex items-center justify-between">
            <div>
              <h3 class="text-sm font-bold text-[#0a0b0d]">BraSeller IA</h3>
              <p class="text-[11px] text-[#5b616e]">Assistente analitico do portal.</p>
            </div>
            <span class="px-2 py-1 bg-[#eef0f3] rounded-full text-[10px] font-bold text-[#5b616e]">Preview</span>
          </div>

          <div id="chat-history-scroll-box" class="flex-grow p-5 space-y-4 overflow-y-auto bg-[#f7f7f7]">
            @for (message of shell.chatMessages(); track message.date) {
              <div class="flex" [class.justify-end]="message.role === 'user'">
                <div
                  class="max-w-[82%] rounded-2xl px-4 py-3 text-xs leading-relaxed"
                  [ngClass]="message.role === 'user'
                    ? 'bg-[#0052ff] text-white rounded-br-md'
                    : 'bg-white border border-[#dee1e6] text-[#0a0b0d] rounded-bl-md'"
                >
                  <div [innerHTML]="shell.renderMarkdown(message.text)"></div>
                  <span class="block text-[9px] opacity-60 mt-2">{{ message.date | date:'HH:mm' }}</span>
                </div>
              </div>
            }
          </div>

          <form [formGroup]="shell.chatForm" (ngSubmit)="shell.sendChatMessage()" class="p-4 border-t border-[#dee1e6] bg-white flex gap-3">
            <input
              type="text"
              formControlName="message"
              placeholder="Pergunte sobre faturamento, taxas ou repasses..."
              class="flex-grow px-4 py-3 text-sm border border-[#dee1e6] rounded-full focus:outline-none focus:border-[#0052ff]"
            >
            <button
              type="submit"
              [disabled]="shell.chatLoading()"
              class="w-12 h-12 rounded-full bg-[#0052ff] text-white flex items-center justify-center disabled:opacity-50"
              title="Enviar pergunta"
            >
              <span class="material-icons text-lg">send</span>
            </button>
          </form>
        </div>

        <aside class="border border-[#dee1e6] rounded-2xl bg-white shadow-xs p-4 h-fit">
          <h3 class="text-xs font-bold text-[#0a0b0d] uppercase tracking-wider mb-3">Sugestoes</h3>
          <div class="space-y-2">
            @for (suggestion of shell.suggestions; track suggestion) {
              <button
                type="button"
                (click)="shell.selectSuggestion(suggestion)"
                class="w-full text-left p-3 rounded-xl bg-[#f7f7f7] hover:bg-[#eef0f3] text-xs text-[#5b616e] transition-colors"
              >
                {{ suggestion }}
              </button>
            }
          </div>
        </aside>
      </section>
    </div>
  `,
})
export class AiPage {
  readonly shell = inject(DashboardShell);
}
