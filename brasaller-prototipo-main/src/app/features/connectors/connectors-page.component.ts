import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ButtonModule} from 'primeng/button';
import {CardModule} from 'primeng/card';
import {TagModule} from 'primeng/tag';
import {ToggleSwitchModule} from 'primeng/toggleswitch';
import {TooltipModule} from 'primeng/tooltip';
import {BrasellerFacade} from '../../core/state/braseller.facade';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-connectors-page',
  standalone: true,
  imports: [ButtonModule, CardModule, FormsModule, TagModule, ToggleSwitchModule, TooltipModule],
  templateUrl: './connectors-page.component.html',
})
export class ConnectorsPageComponent {
  readonly facade = inject(BrasellerFacade);
}
