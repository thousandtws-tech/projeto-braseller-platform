import { computed, inject, Injectable, signal } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { Order, Tenant } from '../../../core/models/user.model';
import { ApiService } from '../../../core/services/api.service';

export interface DashboardState {
  orders: Order[];
  tenant: Tenant | null;
  loading: boolean;
  error: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class DashboardStore {
  private readonly api = inject(ApiService);

  private readonly _orders = signal<Order[]>([]);
  private readonly _tenant = signal<Tenant | null>(null);
  private readonly _loading = signal<boolean>(false);
  private readonly _error = signal<string | null>(null);

  readonly orders = this._orders.asReadonly();
  readonly tenant = this._tenant.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  readonly grossValue = computed(() =>
    this._orders().reduce((current, item) => item.status === 'paid' ? current + item.grossValue : current, 0)
  );

  readonly platformFee = computed(() =>
    this._orders().reduce((current, item) => item.status === 'paid' ? current + item.platformFee : current, 0)
  );

  readonly netValue = computed(() => this.grossValue() - this.platformFee());

  readonly totalOrdersCount = computed(() => this._orders().length);

  loadDashboardData(): void {
    this._loading.set(true);
    this._error.set(null);

    this.api.get<Order[]>('orders')
      .pipe(finalize(() => this._loading.set(false)))
      .subscribe({
        next: (res) => {
          this._orders.set(res || []);
        },
        error: (err) => {
          this._error.set(err.message || 'Erro ao sincronizar repositorios do Dashboard.');
          this._orders.set([]);
        },
      });
  }

  addOrder(order: Order): void {
    this._orders.update(currentList => [order, ...currentList]);
  }

  updateTenant(tenant: Tenant | null): void {
    this._tenant.set(tenant);
  }
}
