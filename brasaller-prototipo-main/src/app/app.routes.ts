import {Routes} from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard',
  },
  {
    path: 'dashboard',
    title: 'BraSeller | Dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard-page.component').then((m) => m.DashboardPageComponent),
  },
  {
    path: 'lancamentos',
    title: 'BraSeller | Lancamentos',
    loadComponent: () => import('./features/orders/orders-page.component').then((m) => m.OrdersPageComponent),
  },
  {
    path: 'ia',
    title: 'BraSeller | IA',
    loadComponent: () =>
      import('./features/assistant/assistant-page.component').then((m) => m.AssistantPageComponent),
  },
  {
    path: 'conectores',
    title: 'BraSeller | Conectores',
    loadComponent: () =>
      import('./features/connectors/connectors-page.component').then((m) => m.ConnectorsPageComponent),
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
