import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Skeleton Loader Card Component
 * Componente mudo genérico para loading progressivo (Performance visual focado em reduzir estresse cognitivo)
 */
@Component({
  selector: 'app-skeleton-loader',
  imports: [CommonModule],
  template: `
    @if (mode() === 'card') {
      <div [class]="'p-6 border border-[#dee1e6] rounded-2xl bg-white shadow-xs animate-pulse ' + className()">
        <div class="h-4 bg-gray-200 rounded w-1/3 mb-4"></div>
        <div class="h-8 bg-gray-200 rounded w-2/3 mb-3"></div>
        <div class="h-3 bg-gray-200 rounded w-1/2"></div>
      </div>
    } @else if (mode() === 'table') {
      <div class="border border-[#dee1e6] rounded-2xl bg-white shadow-xs animate-pulse p-4">
        <div class="h-8 bg-[#f7f7f7] border-b border-[#dee1e6] mb-4 rounded"></div>
        <div class="space-y-3">
          <div class="h-6 bg-gray-100 rounded w-full"></div>
          <div class="h-6 bg-gray-100 rounded w-full"></div>
          <div class="h-6 bg-gray-100 rounded w-full"></div>
          <div class="h-6 bg-gray-100 rounded w-full"></div>
        </div>
      </div>
    } @else {
      <div [class]="'bg-gray-200 rounded animate-pulse ' + className()" [style.height]="height()" [style.width]="width()"></div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SkeletonLoader {
  mode = input<'simple' | 'card' | 'table'>('simple');
  className = input<string>('');
  height = input<string>('20px');
  width = input<string>('100%');
}
