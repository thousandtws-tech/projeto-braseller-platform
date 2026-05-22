import {CurrencyPipe, DatePipe} from '@angular/common';
import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {ButtonModule} from 'primeng/button';
import {CardModule} from 'primeng/card';
import {DialogModule} from 'primeng/dialog';
import {InputNumberModule} from 'primeng/inputnumber';
import {InputTextModule} from 'primeng/inputtext';
import {SelectModule} from 'primeng/select';
import {TableModule} from 'primeng/table';
import {TagModule} from 'primeng/tag';
import {BrasellerFacade} from '../../core/state/braseller.facade';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-orders-page',
  standalone: true,
  imports: [
    ButtonModule,
    CardModule,
    CurrencyPipe,
    DatePipe,
    DialogModule,
    InputNumberModule,
    InputTextModule,
    ReactiveFormsModule,
    SelectModule,
    TableModule,
    TagModule,
  ],
  templateUrl: './orders-page.component.html',
})
export class OrdersPageComponent {
  readonly facade = inject(BrasellerFacade);

  readonly platformOptions = [
    {label: 'Filtro: Todos', value: 'all'},
    {label: 'Mercado Livre', value: 'ml'},
    {label: 'Shopee', value: 'shopee'},
    {label: 'Amazon', value: 'amazon'},
    {label: 'Manual', value: 'manual'},
  ];

  readonly statusOptions = [
    {label: 'Lancamento: Todos', value: 'all'},
    {label: 'CONCLUIDO (Pago)', value: 'paid'},
    {label: 'PENDENTE', value: 'pending'},
    {label: 'CANCELADO', value: 'cancelled'},
  ];

  readonly paymentOptions = [
    {label: 'Pix instantaneo', value: 'PIX'},
    {label: 'Cartao de Credito', value: 'Cartao'},
    {label: 'Boleto', value: 'Boleto'},
  ];

  getStatusLabel(status: string): string {
    if (status === 'paid') return 'CONCLUIDO';
    if (status === 'pending') return 'PENDENTE';
    if (status === 'cancelled') return 'CANCELADO';
    return status.toUpperCase();
  }

  getManualGrossValue(): number {
    const value = this.facade.manualForm.getRawValue();
    return Number(value.qty || 0) * Number(value.unit_price || 0);
  }

  getManualFeeValue(): number {
    return Number(this.facade.manualForm.controls.platform_fee.value || 0);
  }

  getManualNetValue(): number {
    return Math.max(this.getManualGrossValue() - this.getManualFeeValue(), 0);
  }
}
