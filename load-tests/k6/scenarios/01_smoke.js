/**
 * Smoke Test — sanidade básica.
 * 1 VU, 1 minuto. Verifica que todos os endpoints essenciais respondem.
 * Rodar antes de qualquer teste de carga para não desperdiçar tempo.
 *
 * Executar:
 *   k6 run scenarios/01_smoke.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, THRESHOLDS } from '../config.js';
import { login, refresh, logout, bearerHeaders } from '../helpers/auth.js';

export const options = {
  vus: 1,
  duration: '1m',
  thresholds: THRESHOLDS,
};

export default function () {
  // 1. Gateway health
  const health = http.get(`${BASE_URL}/q/health`, { tags: { name: 'gateway_health' } });
  check(health, { 'gateway health: UP': (r) => r.status === 200 });

  // 2. Listar rotas do gateway
  const routes = http.get(`${BASE_URL}/api`, { tags: { name: 'gateway_routes' } });
  check(routes, {
    'gateway routes: status 200': (r) => r.status === 200,
    'gateway routes: tem rotas': (r) => r.json('routes') !== undefined,
  });

  sleep(1);

  // 3. Login → Refresh → Logout
  const session = login();
  if (!session) return;

  sleep(0.5);

  refresh(session.refreshToken);
  sleep(0.5);

  logout(session.refreshToken);
  sleep(2);
}
