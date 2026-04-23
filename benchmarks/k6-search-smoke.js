import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 20,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TENANT = __ENV.TENANT || 'tenant-a';
const QUERY = __ENV.QUERY || 'distributed';

export default function () {
  const res = http.get(`${BASE_URL}/search?q=${QUERY}&size=10`, {
    headers: {
      'X-Tenant-ID': TENANT,
    },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
