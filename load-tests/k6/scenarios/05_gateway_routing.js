/**
 * Gateway Routing Load Test.
 * Foca no throughput do gateway em si — rota /api (GET) sem dependência de Keycloak.
 * Útil para isolar se gargalos estão no gateway ou nos serviços downstream.
 *
 * Executar:
 *   k6 run scenarios/05_gateway_routing.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, THRESHOLDS } from '../config.js';

export const options = {
  stages: [
    { duration: '1m', target: 20 },
    { duration: '3m', target: 100 },
    { duration: '3m', target: 100 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    'http_req_duration{name:gateway_routes}': ['p(95)<300', 'p(99)<800'],
    'http_req_duration{name:gateway_health}': ['p(95)<200'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  // Health (liveness probe simulada)
  const health = http.get(`${BASE_URL}/q/health/live`, { tags: { name: 'gateway_health' } });
  check(health, { 'health/live: 200': (r) => r.status === 200 });

  sleep(0.1);

  // Listagem de rotas (endpoint público, sem auth, exercita DB de rotas do gateway)
  const routes = http.get(`${BASE_URL}/api`, { tags: { name: 'gateway_routes' } });
  check(routes, {
    'routes: 200': (r) => r.status === 200,
    'routes: body tem status UP': (r) => (r.body || '').includes('UP'),
  });

  sleep(0.5);
}
