import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

/**
 * Custom Button Component
 * Componente "Mudo (Dumb)" focado em acessibilidade, reuso e conformidade visual com o design system
 */
@Component({
  selector: 'app-custom-button',
  imports: [CommonModule, MatIconModule],
  template: `
    <button
      [id]="id()"
      [type]="type()"
      [disabled]="disabled() || loading()"
      (click)="clicked.emit($event)"
      [class]="getButtonClasses()"
      attr.aria-label="{{ ariaLabel() || text() }}"
    >
      @if (loading()) {
        <span class="w-4 h-4 rounded-full border-2 border-current border-t-transparent animate-spin inline-block mr-2" aria-hidden="true"></span>
        <span class="font-semibold">{{ loadingText() || 'Aguarde...' }}</span>
      } @else {
        @if (icon() && iconPosition() === 'left') {
          <mat-icon class="mr-1.5 text-lg flex items-center justify-center" aria-hidden="true">{{ icon() }}</mat-icon>
        }
        <span class="font-semibold">{{ text() }}</span>
        @if (icon() && iconPosition() === 'right') {
          <mat-icon class="ml-1.5 text-lg flex items-center justify-center" aria-hidden="true">{{ icon() }}</mat-icon>
        }
      }
    </button>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CustomButton {
  // Inputs via Angular Signals (New input signature style)
  id = input<string>('btn-' + Math.floor(Math.random() * 1000));
  text = input.required<string>();
  type = input<'button' | 'submit' | 'reset'>('button');
  variant = input<'primary' | 'secondary' | 'danger' | 'ghost'>('primary');
  size = input<'sm' | 'md' | 'lg'>('md');
  disabled = input<boolean>(false);
  loading = input<boolean>(false);
  loadingText = input<string>('');
  icon = input<string>('');
  iconPosition = input<'left' | 'right'>('left');
  ariaLabel = input<string>('');

  // Outputs reativos utilizando a nova API native de output()
  clicked = output<MouseEvent>();

  /**
   * Constrói e injeta dinamicamente as classes de layout artístico do Tailwind CSS
   */
  getButtonClasses(): string {
    const base = 'inline-flex items-center justify-center rounded-full transition-all duration-200 outline-none focus:ring-2 focus:ring-offset-2 font-sans cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed';
    
    // Variantes cromáticas artísticas
    const variants = {
      primary: 'bg-[#0052ff] hover:bg-[#003ecc] text-white focus:ring-[#0052ff]/50 border border-transparent shadow-xs',
      secondary: 'bg-white hover:bg-[#f7f7f7] text-[#0a0b0d] border border-[#dee1e6] focus:ring-[#dee1e6]/60 shadow-xs',
      danger: 'bg-[#cf202f] hover:bg-[#b81c29] text-white focus:ring-[#cf202f]/50 border border-transparent shadow-xs',
      ghost: 'bg-transparent hover:bg-black/5 text-[#5b616e] focus:ring-black/5 border border-transparent'
    };

    // Escalonamentos dimensionais ergonômicos
    const sizes = {
      sm: 'px-3 py-1.5 text-xs text-medium',
      md: 'px-5 py-2 text-xs font-semibold',
      lg: 'px-6 py-2.5 text-sm font-semibold'
    };

    return `${base} ${variants[this.variant()]} ${sizes[this.size()]}`;
  }
}
