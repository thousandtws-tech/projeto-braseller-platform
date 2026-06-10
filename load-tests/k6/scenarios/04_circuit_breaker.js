/**
 * Circuit Breaker Verification Test.
 * Valida que o SmallRye Circuit Breaker está funcionando:
 * 1. Spike intencional de logins com credenciais inválidas → força falhas
 * 2. Observa transição CLOSED → OPEN (503 com circuit_breaker_open)
 * 3. Após delay do CB (30s), VUs voltam a tentar → observa HALF_OPEN → CLOSED
 *
 * NÃO rodar em produção com usuários reais. Usar ambiente de staging.
 *
 * Executar:
 *   k6 run scenarios/04_circuit_breaker.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import { BASE_URL } from '../config.js';

const cbOpenCount = new Counter('cb_open_responses');
const cbTripRate = new Rate('cb_trip_rate');

export const options = {
  scenarios: {
    // Fase 1: spike com credenciais inválidas para induzir falhas no Keycloak
    induce_failures: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 30 }, // spike rápido
        { duration: '60s', target: 30 }, // sustenta para acumular falhas
        { duration: '10s', target: 0 },
      ],
      gracefulRampDown: '5s',
      tags: { phase: 'induce' },
    },
    // Fase 2: após o CB abrir, verifica fail-fast e depois recuperação
    verify_recovery: {
      executor: 'constant-vus',
      vus: 5,
      duration: '3m',
      startTime: '2m', // começa quando o CB já deve estar aberto
      tags: { phase: 'verify' },
    },
  },
  thresholds: {
    // Queremos VER o CB abrir — sem threshold restritivo aqui
    http_req_failed: ['rate<0.80'],
  },
};

// Fase 1: força falhas com credenciais inválidas
export function induce_failures() {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email: 'invalid@test.com', password: 'wrong_password' }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'auth_login_fail' } },
  );

  const isCbOpen = res.status === 503 && (res.body || '').includes('circuit_breaker_open');
  cbTripRate.add(isCbOpen ? 1 : 0);
  if (isCbOpen) cbOpenCount.add(1);

  check(res, {
    'credencial inválida retorna 401 ou CB 503': (r) => r.status === 401 || r.status === 503,
  });

  sleep(0.2);
}

// Fase 2: testa com credencial válida — deve falhar rápido (CB aberto) e depois recuperar
export function verify_recovery() {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email: 'loadtest@brasaller.com', password: 'LoadTest@123' }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'auth_login_valid' } },
  );

  const isCbOpen = res.status === 503 && (res.body || '').includes('circuit_breaker_open');
  cbTripRate.add(isCbOpen ? 1 : 0);
  if (isCbOpen) {
    cbOpenCount.add(1);
    // CB aberto = fail-fast correto. Aguarda o half-open window (30s configurado)
    console.log(`[CB OPEN] status=${res.status} — aguardando recovery`);
  }

  check(res, {
    'recovery: 200 ou CB 503 esperado': (r) => r.status === 200 || r.status === 503,
  });

  sleep(2);
}

export default function () {
  // Entrada padrão — só executa se não usar scenarios com executors separados
  induce_failures();
}
