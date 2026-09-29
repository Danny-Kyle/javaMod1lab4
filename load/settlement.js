import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: 200,
  duration: '60s',
  summaryTrendStats: ['avg', 'p(50)', 'p(95)', 'p(99)'],
};

// Seeds one payment for MR-4471 (a POST, so it does not touch the fee table).
export function setup() {
  const res = http.post(
    `${BASE}/payments`,
    JSON.stringify({ merchantId: 'MR-4471', amountMinor: 128450, currency: 'GBP' }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  check(res, { 'seed payment created': (r) => r.status === 201 });
}

export default function () {
  const res = http.get(`${BASE}/payments/settlement?merchantId=MR-4471`);
  check(res, { 'status is 200': (r) => r.status === 200 });
}
