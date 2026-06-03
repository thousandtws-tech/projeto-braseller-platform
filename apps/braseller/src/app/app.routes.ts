import { Routes } from '@angular/router';

/**
 * O componente raiz controla o fluxo de autenticação e dashboard.
 * As rotas abaixo mantêm URLs diretas e callbacks OAuth sem redirecionar
 * para telas paralelas antigas.
 */
export const routes: Routes = [
  { path: '', children: [] },
  { path: 'login', children: [] },
  { path: 'auth/callback', children: [] },
  { path: '**', children: [] },
];
