import {DOCUMENT, DatePipe} from '@angular/common';
import {ChangeDetectionStrategy, Component, effect, inject, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormsModule} from '@angular/forms';
import {ReactiveFormsModule} from '@angular/forms';
import {NavigationEnd, Router, RouterLink, RouterOutlet} from '@angular/router';
import {filter} from 'rxjs';
import {AvatarModule} from 'primeng/avatar';
import {BadgeModule} from 'primeng/badge';
import {ButtonModule} from 'primeng/button';
import {DialogModule} from 'primeng/dialog';
import {InputTextModule} from 'primeng/inputtext';
import {MenuModule} from 'primeng/menu';
import {MessageModule} from 'primeng/message';
import {PopoverModule} from 'primeng/popover';
import {ProgressBarModule} from 'primeng/progressbar';
import {SelectModule} from 'primeng/select';
import {TagModule} from 'primeng/tag';
import {ToolbarModule} from 'primeng/toolbar';
import {TooltipModule} from 'primeng/tooltip';
import {AppSection, UserRole} from '../core/models/braseller.models';
import {BrasellerFacade} from '../core/state/braseller.facade';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-shell',
  standalone: true,
  imports: [
    AvatarModule,
    BadgeModule,
    ButtonModule,
    DatePipe,
    DialogModule,
    FormsModule,
    InputTextModule,
    MenuModule,
    MessageModule,
    PopoverModule,
    ProgressBarModule,
    ReactiveFormsModule,
    RouterLink,
    RouterOutlet,
    SelectModule,
    TagModule,
    ToolbarModule,
    TooltipModule,
  ],
  templateUrl: './app-shell.component.html',
})
export class AppShellComponent {
  readonly facade = inject(BrasellerFacade);
  private readonly document = inject(DOCUMENT);
  private readonly router = inject(Router);

  readonly currentSection = signal<AppSection>(this.getSectionFromUrl(this.router.url));

  readonly uiZoom = signal<'auto' | '90' | '100' | '110'>('auto');

  readonly zoomOptions = [
    {label: 'Zoom auto', value: 'auto'},
    {label: '90%', value: '90'},
    {label: '100%', value: '100'},
    {label: '110%', value: '110'},
  ];

  private readonly zoomEffect = effect(() => {
    this.document.documentElement.setAttribute('data-ui-zoom', this.uiZoom());
  });

  readonly roleOptions: {label: string; value: UserRole}[] = [
    {label: 'Vendedor principal', value: 'seller'},
    {label: 'Vendedor secundario', value: 'seller_sec'},
    {label: 'Contador', value: 'accountant'},
  ];

  readonly navItems: {label: string; icon: string; section: AppSection}[] = [
    {label: 'Dashboard', icon: 'pi pi-chart-line', section: 'dashboard'},
    {label: 'Lancamentos', icon: 'pi pi-receipt', section: 'orders'},
    {label: 'BraSeller IA', icon: 'pi pi-sparkles', section: 'chat'},
    {label: 'Conectores', icon: 'pi pi-th-large', section: 'connectors'},
  ];

  readonly sectionRoutes: Record<AppSection, string> = {
    dashboard: '/dashboard',
    orders: '/lancamentos',
    chat: '/ia',
    connectors: '/conectores',
  };

  readonly sectionTitles: Record<AppSection, string> = {
    dashboard: 'Dashboard consolidado',
    orders: 'Lancamentos fiscais',
    chat: 'BraSeller Inteligencia Analitica',
    connectors: 'Arquitetura modular',
  };

  readonly licensePlans = [
    {
      name: 'Basico',
      price: '49',
      description: 'Ideal para iniciantes no Mercado Livre que precisam do banco consolidado basico.',
      action: 'Escolher Basico',
      plan: 'Basico',
      variant: 'basic',
      features: [
        {label: '1 Conector Ativo', enabled: true},
        {label: 'Sincronia Rapida', enabled: false},
        {label: 'Fechamento Contador XML', enabled: false},
      ],
    },
    {
      name: 'PRO Premium',
      price: '119',
      description: 'Libere o conector Shopee e tenha acesso a suporte de estimativa analitica IA.',
      action: 'Assinar PRO',
      plan: 'Pro',
      variant: 'pro',
      recommended: true,
      features: [
        {label: '2 Conectores Simultaneos', enabled: true},
        {label: 'Relatorios Contabeis Completos', enabled: true},
        {label: 'Insights Preditivos Ativos', enabled: true},
      ],
    },
    {
      name: 'Corporativo',
      price: '289',
      description: 'Acople Amazon, equipe ilimitada de faturadores e acesso direto do contador.',
      action: 'Contratar Agencia',
      plan: 'Agencia',
      variant: 'corporate',
      features: [
        {label: 'Conectores Ilimitados', enabled: true},
        {label: 'Suporte Premium em 15 minutos', enabled: true},
        {label: 'Sincronia Multi-Tenant Ilimitada', enabled: true},
      ],
    },
  ];

  constructor() {
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe((event) => {
        const section = this.getSectionFromUrl(event.urlAfterRedirects);
        this.currentSection.set(section);
        this.facade.activeSection.set(section);
      });
  }

  goTo(section: AppSection) {
    this.facade.activeSection.set(section);
    void this.router.navigateByUrl(this.sectionRoutes[section]);
  }

  private getSectionFromUrl(url: string): AppSection {
    if (url.startsWith('/lancamentos')) return 'orders';
    if (url.startsWith('/ia')) return 'chat';
    if (url.startsWith('/conectores')) return 'connectors';
    return 'dashboard';
  }
}
