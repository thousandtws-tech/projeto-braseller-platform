import {DatePipe} from '@angular/common';
import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {ButtonModule} from 'primeng/button';
import {CardModule} from 'primeng/card';
import {ChipModule} from 'primeng/chip';
import {ProgressSpinnerModule} from 'primeng/progressspinner';
import {TextareaModule} from 'primeng/textarea';
import {BrasellerFacade} from '../../core/state/braseller.facade';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-assistant-page',
  standalone: true,
  imports: [ButtonModule, CardModule, ChipModule, DatePipe, ProgressSpinnerModule, ReactiveFormsModule, TextareaModule],
  templateUrl: './assistant-page.component.html',
})
export class AssistantPageComponent {
  readonly facade = inject(BrasellerFacade);
}
