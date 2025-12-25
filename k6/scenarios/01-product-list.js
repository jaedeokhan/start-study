import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = 'http://localhost:8080/api/v1';

const TARGETS = {
  productList: {
    tps: 1000,
    p95: 1800,
    p99: 2000,
    errorRate: 0.001,
  },
};

const errorRate = new Rate('errors');
const responseTime = new Trend('response_time');

export const options = {
  scenarios: {
    product_list: {
      executor: 'constant-arrival-rate',
      rate: TARGETS.productList.tps, // TPS
      timeUnit: '1s',
      duration: '5m',
      preAllocatedVUs: 20,
      maxVUs: 1000,
    },
  },
  thresholds: {
    http_req_duration: [
      `p(95)<${TARGETS.productList.p95}`,
      `p(99)<${TARGETS.productList.p99}`,
    ],
    http_req_failed: [`rate<${TARGETS.productList.errorRate}`],
    errors: [`rate<${TARGETS.productList.errorRate}`],
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/products`);

  const success = check(res, {
    'status is 200': (r) => r.status === 200,
  });

  errorRate.add(!success);
  responseTime.add(res.timings.duration);
}

