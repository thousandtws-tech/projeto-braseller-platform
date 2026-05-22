import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {ButtonModule} from 'primeng/button';
import {CardModule} from 'primeng/card';
import {DividerModule} from 'primeng/divider';
import {InputTextModule} from 'primeng/inputtext';
import {MessageModule} from 'primeng/message';
import {PasswordModule} from 'primeng/password';
import {TabsModule} from 'primeng/tabs';
import {BrasellerFacade} from '../../core/state/braseller.facade';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-auth-page',
  standalone: true,
  imports: [ButtonModule, CardModule, DividerModule, InputTextModule, MessageModule, PasswordModule, ReactiveFormsModule, TabsModule],
  templateUrl: './auth-page.component.html',
})
export class AuthPageComponent {
  readonly facade = inject(BrasellerFacade);
}
