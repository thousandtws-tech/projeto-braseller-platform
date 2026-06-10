/**
 * Stress Test — encontrar o ponto de ruptura.
 * Aumenta VUs progressivamente até 200 para observar quando p(95) e taxa de erro
 * começam a degradar. Não tem thresholds restritivos — objetivo é coletar dados.
 *
 * Executar:
 *   k6 run scenarios/03_stress.js
 *   k6 run --out json=results/stress.json scenarios/03_stress.js
 *
 * Sinais de ruptura a observar:
 *   - http_req_duration p(95) > 5s
 *   - http_req_failed rate > 10%
 *   - Circuit breaker trips (503 responses com body "circuit_breaker_open")
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { BASE_URL } from '../config.js';
import { login } from '../helpers/auth.js';

const circuitBreakerTrips = new Counter('circuit_breaker_trips');
const loginLatency = new Trend('login_latency_ms', true);

export const options = {
  stages: [
    { duration: '2m', target: 20 },
    { duration: '3m', target: 50 },
    { duration: '3m', target: 100 },
    { duration: '3m', target: 150 },
    { duration: '3m', target: 200 },
    { duration: '3m', target: 200 }, // pico sustentado
    { duration: '3m', target: 0 },   // cooldown
  ],
  thresholds: {
    // Apenas marca falha se ultrapassar limites críticos (não bloquear coleta de dados)
    http_req_failed: ['rate<0.30'],
  },
};

export default function () {
  const start = Date.now();
  const session = login();
  loginLatency.add(Date.now() - start);

  if (!session) {
    // Verificar se é circuit breaker aberto
    const res = http.post(
      `${BASE_URL}/api/auth/login`,
      JSON.stringify({ email: 'loadtest@brasaller.com', password: 'LoadTest@123' }),
      { headers: { 'Content-Type': 'application/json' }, tags: { name: 'auth_login' } },
    );
    if (res.status === 503 && res.body.includes('circuit_breaker_open')) {
      circuitBreakerTrips.add(1);
    }
    sleep(1);
    return;
  }

  const health = http.get(`${BASE_URL}/q/health`, { tags: { name: 'gateway_health' } });
  check(health, { 'health: UP': (r) => r.status === 200 });

  sleep(1);
}
