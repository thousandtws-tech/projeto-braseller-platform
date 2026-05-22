import {CurrencyPipe, DecimalPipe, KeyValuePipe} from '@angular/common';
import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {ButtonModule} from 'primeng/button';
import {CardModule} from 'primeng/card';
import {ProgressBarModule} from 'primeng/progressbar';
import {TableModule} from 'primeng/table';
import {TagModule} from 'primeng/tag';
import {BrasellerFacade} from '../../core/state/braseller.facade';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [ButtonModule, CardModule, CurrencyPipe, DecimalPipe, KeyValuePipe, ProgressBarModule, TableModule, TagModule],
  templateUrl: './dashboard-page.component.html',
})
export class DashboardPageComponent {
  readonly facade = inject(BrasellerFacade);
}
