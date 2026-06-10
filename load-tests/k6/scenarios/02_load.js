/**
 * Load Test — carga típica de produção.
 * Simula fluxo real: login → operações autenticadas → logout.
 * Rampa gradual até 50 VUs para observar throughput e latência estáveis.
 *
 * Executar:
 *   k6 run scenarios/02_load.js
 *   k6 run --out json=results/load.json scenarios/02_load.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, THRESHOLDS } from '../config.js';
import { login, refresh, bearerHeaders } from '../helpers/auth.js';

export const options = {
  stages: [
    { duration: '2m', target: 10 },  // aquecimento
    { duration: '5m', target: 50 },  // carga alvo
    { duration: '3m', target: 50 },  // sustentação
    { duration: '2m', target: 0 },   // cooldown
  ],
  thresholds: {
    ...THRESHOLDS,
    'http_req_duration{name:auth_login}': ['p(95)<3000'],
    'http_req_duration{name:gateway_health}': ['p(95)<500'],
    'http_req_duration{name:gateway_routes}': ['p(95)<1000'],
  },
};

export default function () {
  // Health check leve (simula monitoramento de frontend)
  const health = http.get(`${BASE_URL}/q/health`, { tags: { name: 'gateway_health' } });
  check(health, { 'health: UP': (r) => r.status === 200 });

  sleep(0.5);

  // Fluxo de autenticação (mais pesado — envolve Keycloak + user-service)
  const session = login();
  if (!session) {
    sleep(2);
    return;
  }

  sleep(1);

  // Listar rotas (GET simples, exercita o gateway)
  const routes = http.get(
    `${BASE_URL}/api`,
    { headers: bearerHeaders(session.accessToken), tags: { name: 'gateway_routes' } },
  );
  check(routes, { 'routes: status 200': (r) => r.status === 200 });

  sleep(1);

  // Renovar token (simula sessão longa)
  refresh(session.refreshToken);

  sleep(2);
}
