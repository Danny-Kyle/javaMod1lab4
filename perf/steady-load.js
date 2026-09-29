import http from 'k6/http';
import { check, fail } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const MERCHANT = __ENV.MERCHANT || 'MR-9001';

// Constant ARRIVAL rate: k6 starts RATE new requests per second no matter how slow
// the service is. If the service cannot keep up, k6 reports dropped_iterations.
export const options = {
  scenarios: {
    steady: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 100),
      timeUnit: '1s',
      duration: __ENV.DURATION || '10m',
      preAllocatedVUs: 100,
      maxVUs: 500,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
  summaryTrendStats: ['avg', 'p(50)', 'p(95)', 'p(99)', 'max'],
};

// Runs once before the load: proves the merchant exists and prints the answer,
// so you can confirm the number is identical before and after any change.
export function setup() {
  const res = http.get(`${BASE}/payments/settlement?merchantId=${MERCHANT}`);
  if (res.status !== 200) {
    fail(`Seed data missing? GET settlement for ${MERCHANT} returned ${res.status}`);
  }
  console.log(`SETUP answer for ${MERCHANT}: ${res.body}`);
}

export default function () {
  const res = http.get(`${BASE}/payments/settlement?merchantId=${MERCHANT}`);
  check(res, { 'status is 200': (r) => r.status === 200 });
}
