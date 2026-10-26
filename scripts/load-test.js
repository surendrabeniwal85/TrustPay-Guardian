// ==============================================================================
// TrustPay Guardian — k6 Automated Performance & Concurrency Load Test
// Target: http://localhost:8080 (or custom BASE_URL)
// Command: k6 run scripts/load-test.js
// ==============================================================================

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom Metrics
const successfulTransfers = new Counter('successful_transfers');
const fraudHeldTransfers = new Counter('fraud_held_transfers');
const rateLimitedRequests = new Counter('rate_limited_requests');
const transferLatency = new Trend('transfer_latency_ms');
const errorRate = new Rate('error_rate');

// Workload Profile: Ramp-up, Sustained Load, and Graceful Ramp-down
export const options = {
  stages: [
    { duration: '30s', target: 20 },  // Warm-up to 20 virtual users (VUs)
    { duration: '1m',  target: 50 },  // Ramp to 50 VUs (sustained load)
    { duration: '30s', target: 100 }, // Spike to 100 VUs (concurrency stress)
    { duration: '30s', target: 0 },   // Cool-down
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500', 'p(99)<1200'], // 95% of requests under 500ms
    'error_rate': ['rate<0.02'],                       // Less than 2% server errors
    'transfer_latency_ms': ['p(90)<400'],              // 90% of transfer transactions under 400ms
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

// Seeded Customer Credentials
const CUSTOMER_EMAIL = 'john.doe@trustpay.internal';
const CUSTOMER_PASSWORD = 'Password123!';

export default function () {
  let authToken = '';

  // 1. Healthcheck & Public Stats (Unauthenticated)
  group('01_Public_Health_And_Stats', function () {
    const healthRes = http.get(`${BASE_URL.replace('/api/v1', '')}/actuator/health`);
    check(healthRes, {
      'healthcheck status is 200': (r) => r.status === 200,
    }) || errorRate.add(1);

    const statsRes = http.get(`${BASE_URL}/public/stats`);
    check(statsRes, {
      'public stats status is 200': (r) => r.status === 200,
      'stats contains totalTransfers': (r) => r.json('data.totalTransfers') !== undefined,
    }) || errorRate.add(1);
  });

  sleep(1);

  // 2. Authentication & JWT Extraction
  group('02_Customer_Authentication', function () {
    const loginPayload = JSON.stringify({
      email: CUSTOMER_EMAIL,
      password: CUSTOMER_PASSWORD,
    });

    const loginRes = http.post(`${BASE_URL}/auth/login`, loginPayload, {
      headers: { 'Content-Type': 'application/json' },
    });

    if (loginRes.status === 429) {
      rateLimitedRequests.add(1);
      return;
    }

    const loginOk = check(loginRes, {
      'login status is 200': (r) => r.status === 200,
      'accessToken received': (r) => r.json('data.accessToken') !== undefined,
    });

    if (!loginOk) {
      errorRate.add(1);
      return;
    }

    authToken = loginRes.json('data.accessToken');
  });

  if (!authToken) {
    sleep(1);
    return;
  }

  const authHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${authToken}`,
  };

  sleep(1);

  // 3. Account Balance Query
  let sourceAccountId = 1;
  group('03_Query_Accounts', function () {
    const accRes = http.get(`${BASE_URL}/accounts`, { headers: authHeaders });
    check(accRes, {
      'accounts status is 200': (r) => r.status === 200,
      'user has accounts': (r) => r.json('data.length') > 0,
    }) || errorRate.add(1);

    const accounts = accRes.json('data');
    if (accounts && accounts.length > 0) {
      sourceAccountId = accounts[0].id;
    }
  });

  sleep(1);

  // 4. Money Transfer with Idempotency Key & Concurrency Locking
  group('04_Execute_Transfer', function () {
    const idempotencyKey = 'k6-' + Math.random().toString(36).substring(2, 15) + '-' + Date.now();
    const amount = (Math.floor(Math.random() * 50) + 1).toFixed(2); // Random ₹1 to ₹50

    const transferPayload = JSON.stringify({
      sourceAccountId: sourceAccountId,
      destinationAccountNumber: 'TPG1000000002', // Seeded Jane Smith account
      amount: parseFloat(amount),
      note: 'k6 automated concurrency test',
    });

    const transferHeaders = Object.assign({}, authHeaders, {
      'Idempotency-Key': idempotencyKey,
    });

    const startTime = Date.now();
    const transferRes = http.post(`${BASE_URL}/transactions/transfer`, transferPayload, {
      headers: transferHeaders,
    });
    const latency = Date.now() - startTime;
    transferLatency.add(latency);

    if (transferRes.status === 429) {
      rateLimitedRequests.add(1);
      return;
    }

    const transferOk = check(transferRes, {
      'transfer status is 200 or 201': (r) => r.status === 200 || r.status === 201,
      'has valid reference': (r) => r.json('data.reference') !== undefined,
    });

    if (transferOk) {
      const status = transferRes.json('data.status');
      if (status === 'APPROVED_SIMULATED') {
        successfulTransfers.add(1);
      } else if (status === 'PENDING_REVIEW') {
        fraudHeldTransfers.add(1);
      }
    } else {
      errorRate.add(1);
    }
  });

  sleep(1);
}
