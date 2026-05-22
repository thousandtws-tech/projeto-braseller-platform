import {ChangeDetectionStrategy, Component, inject, OnInit} from '@angular/core';
import {AuthPageComponent} from './features/auth/auth-page.component';
import {AppShellComponent} from './layout/app-shell.component';
import {BrasellerFacade} from './core/state/braseller.facade';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-root',
  standalone: true,
  imports: [AppShellComponent, AuthPageComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  readonly facade = inject(BrasellerFacade);

  ngOnInit() {
    this.facade.init();
  }
}
