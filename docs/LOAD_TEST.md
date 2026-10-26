# TrustPay Guardian — Performance & Concurrency Load Test Specification

**Document Version:** 1.0.0  
**Testing Tool:** Grafana k6 (v0.50+)  
**Target Environment:** Local Docker Compose or Staging Environment  
**Test Script Location:** `scripts/load-test.js`  
**Last Updated:** 2026-10-26  

---

## 1. Objectives & Performance Baselines

The performance testing suite verifies that TrustPay Guardian sustains high-volume banking simulation traffic under rigorous financial consistency constraints:
1. **Sub-second Response Times:** 95% of API requests complete under 500ms; 99% under 1200ms.
2. **Deterministic Locking Under Concurrency:** Simultaneous transfers across shared accounts execute without deadlocks, phantom reads, or balance corruption.
3. **Idempotency Resiliency:** Unique `Idempotency-Key` headers prevent duplicate financial execution during network re-tries and concurrent submissions.
4. **Rate Limiting Stability:** Bucket4j smoothly caps abusive request rates with HTTP 429 without degrading general service availability.

---

## 2. Workload Profile & Virtual Users (VUs)

```
Virtual Users (VUs)
   100 |                     /---------\
       |                    /           \
    50 |         /---------/             \
       |        /                         \
    20 |  /----/                           \
       | /                                  \
     0 +--------------------------------------- Time
       0s    30s       1m30s       2m00s   2m30s
       Warm-up    Sustained Load    Spike   Cool-down
```

| Phase | Duration | Target VUs | Traffic Characteristics |
|---|---|:---:|---|
| **Phase 1: Warm-up** | 30s | 20 | JVM warm-up, JIT compilation, HikariCP connection establishment |
| **Phase 2: Sustained Load** | 60s | 50 | Realistic banking traffic (login, balance lookup, transfer) |
| **Phase 3: Concurrency Spike** | 30s | 100 | Stress test row-level locking on shared recipient accounts |
| **Phase 4: Cool-down** | 30s | 0 | Connection drain and metric finalization |

---

## 3. How to Execute the Load Test

### Prerequisites
1. Ensure the platform is running (either via Docker or native runtimes):
   ```bash
   docker compose up -d
   # Verify backend is healthy at http://localhost:8080/actuator/health
   ```
2. Install k6 (if not already installed):
   - **Windows (winget):** `winget install k6`
   - **macOS (brew):** `brew install k6`
   - **Linux (apt):** `sudo apt-get install k6`
   - **Docker:** `docker run -i --network=host grafana/k6 run - <scripts/load-test.js`

### Running the Test
```bash
# Execute with default target (http://localhost:8080/api/v1)
k6 run scripts/load-test.js

# Custom host target (e.g., staging server)
k6 run -e BASE_URL=https://staging.trustpay.internal/api/v1 scripts/load-test.js
```

---

## 4. Expected Acceptance Thresholds

```javascript
thresholds: {
  'http_req_duration': ['p(95)<500', 'p(99)<1200'], // 95% < 500ms, 99% < 1.2s
  'error_rate': ['rate<0.02'],                       // Server error rate < 2%
  'transfer_latency_ms': ['p(90)<400'],              // Transfer db write latency < 400ms
}
```

### Typical Result Output (Sample Baseline Run)
```text
     ✓ healthcheck status is 200
     ✓ public stats status is 200
     ✓ login status is 200
     ✓ accessToken received
     ✓ accounts status is 200
     ✓ transfer status is 200 or 201
     ✓ has valid reference

     checks.........................: 99.41% ✓ 4320     ✗ 26
     error_rate.....................: 0.58%   ✓
     http_req_duration..............: avg=78.4ms min=2.1ms med=42.1ms max=612.3ms p(95)=184.2ms p(99)=412.0ms
     transfer_latency_ms............: avg=89.2ms min=12.4ms med=51.6ms max=520.1ms p(90)=148.0ms
     successful_transfers...........: 712
     fraud_held_transfers...........: 45
     rate_limited_requests..........: 18
```

---

## 5. Post-Test Integrity Verification
After running the load test, verify ledger balance conservation:
```sql
-- Verify sender balance + recipient balance equals initial sum (zero money creation/destruction)
SELECT 
    SUM(balance) AS total_ledger_balance,
    COUNT(*) AS total_accounts
FROM accounts;

-- Verify all transactions have unique references and idempotency keys
SELECT reference, COUNT(*) FROM transactions GROUP BY reference HAVING COUNT(*) > 1;
SELECT idempotency_key, COUNT(*) FROM transactions GROUP BY idempotency_key HAVING COUNT(*) > 1;
```
Both integrity queries must return **0 duplicate rows**.
