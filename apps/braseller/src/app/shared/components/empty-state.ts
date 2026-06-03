import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

/**
 * Empty State Component
 * Componente genérico para exibições em listas vazias, incentiva engajamento e ações do usuário.
 */
@Component({
  selector: 'app-empty-state',
  imports: [CommonModule, MatIconModule],
  template: `
    <div [id]="id()" class="flex flex-col items-center justify-center p-8 md:p-12 text-center bg-white border border-dashed border-[#dee1e6] rounded-2xl">
      <div class="w-14 h-14 rounded-full bg-[#f7f7f7] text-[#7c828a] flex items-center justify-center mb-4">
        <mat-icon class="text-2xl">{{ icon() }}</mat-icon>
      </div>
      <h3 class="text-sm font-bold text-[#0a0b0d] font-display tracking-tight leading-none mb-1">{{ title() }}</h3>
      <p class="text-[11px] text-[#5b616e] max-w-[280px] mb-4 leading-normal">{{ description() }}</p>
      
      @if (actionText()) {
        <button
          [id]="id() + '-btn'"
          type="button"
          (click)="actionClicked.emit()"
          class="inline-flex items-center gap-1.5 px-4 py-1.5 bg-[#eef0f3] hover:bg-[#dee1e6] text-[11px] font-bold text-gray-700 rounded-full transition-colors cursor-pointer"
        >
          <mat-icon class="text-sm">{{ actionIcon() }}</mat-icon>
          <span>{{ actionText() }}</span>
        </button>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmptyState {
  id = input<string>('empty-state-' + Math.floor(Math.random() * 1000));
  title = input.required<string>();
  description = input.required<string>();
  icon = input<string>('assignment_late');
  actionText = input<string>('');
  actionIcon = input<string>('add_circle');

  actionClicked = output<void>();
}
