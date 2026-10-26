# TrustPay Guardian — Operational Runbook & Production Playbook

**Target System:** TrustPay Guardian: Fraud-Aware Digital Banking Platform  
**Target Environments:** Staging & Production (Docker / Kubernetes)  
**Classification:** Internal Engineering & DevOps Operations  
**Last Updated:** 2026-10-26  

---

## 1. System Health & Monitoring

### 1.1 Healthcheck Endpoints
| Component | Endpoint / Probe | Protocol | Expected Status | Action on Failure |
|---|---|---|---|---|
| **Frontend Web** | `http://<domain>/health` | HTTP GET | `200 OK` (body: `OK\n`) | Check Nginx process and container restart policy |
| **Backend API** | `http://<domain>/actuator/health` | HTTP GET | `200 OK` (`{"status":"UP"}`) | Inspect JVM logs, database connection pool, and disk space |
| **Database** | `mysqladmin ping -h localhost` | CLI | `mysqld is alive` | Check MySQL container logs, disk volume, and memory limits |
| **Mail Service** | `http://<mailhog-host>:8025/` | HTTP GET | `200 OK` | Verify MailHog container status (dev) or external SMTP relay |

### 1.2 Key Operational Metrics (Prometheus / Actuator)
- `hikaricp.connections.active`: Active DB connections. Alert if >80% of `maximum-pool-size` for >2 minutes.
- `hikaricp.connections.pending`: Threads waiting for a DB connection. Alert if >0 for >30 seconds (indicates pool exhaustion).
- `http.server.requests`: Request rate, 5xx errors, and latency. Alert if 5xx rate >1% over 5 minutes or p99 latency >1500ms.
- `jvm.memory.used`: Heap memory consumption. Alert if heap utilization >85% after garbage collection.

---

## 2. Emergency Incident Response Playbooks

### Playbook 1: Database Connection Pool Exhaustion (HikariCP)
**Symptoms:** Requests timing out with `SQLTransientConnectionException: HikariPool - Connection is not available, request timed out after 30000ms`.
1. **Immediate Triage:**
   ```bash
   # Check active connections inside MySQL
   docker exec -it trustpay-mysql mysql -u root -p -e "SHOW PROCESSLIST;"
   ```
2. **Identify Long-Running Queries:**
   Look for transactions held open or locks waiting in `information_schema.innodb_trx` or `sys.innodb_lock_waits`.
3. **Remediation:**
   - Kill unresponsive long-running queries: `KILL <process_id>;`.
   - If caused by traffic spike, temporarily adjust pool size in `docker-compose.yml` or container environment:
     `HIKARI_MAX_POOL_SIZE=25` (ensure MySQL `max_connections` is sized accordingly).
   - Restart backend container gracefully: `docker compose restart backend`.

### Playbook 2: Deadlock Spike During Peak Transfers
**Symptoms:** Error logs showing `DeadlockLoserDataAccessException` or `LockAcquisitionException`.
1. **Diagnosis:**
   TrustPay Guardian executes deterministic ascending-ID row locking (`Math.min(acc1, acc2)` then `Math.max(acc1, acc2)`). Deadlocks indicate external raw queries or concurrent administrative operations locking accounts out of order.
2. **Review InnoDB Deadlock Status:**
   ```bash
   docker exec -it trustpay-mysql mysql -u root -p -e "SHOW ENGINE INNODB STATUS\G"
   ```
3. **Remediation:**
   - `TransferService` includes bounded exponential backoff retry (up to 3 attempts). Verify retries succeed without customer impact.
   - If an account is subject to runaway concurrent debits, execute an administrative freeze:
     ```bash
     curl -X PATCH http://localhost:8080/api/v1/admin/accounts/<account_id>/freeze \
       -H "Authorization: Bearer <admin_token>"
     ```

### Playbook 3: Brute Force Login Attack on Staff or Customers
**Symptoms:** Spike in HTTP 401 and 429 responses; repeated failed logins logged with `AUTH_FAILED`.
1. **Automatic Defense in Place:**
   - Bucket4j Rate Limiting restricts login attempts to 10 requests per minute per IP address (returns `429 Too Many Requests`).
   - 5 consecutive failed logins trigger automatic 15-minute account lockouts (`User.lockedUntil = NOW() + 15m`).
   - Fraud Rule R7 flags subsequent transfers on recently attacked accounts.
2. **Manual Intervention:**
   - If attacking from a known malicious IP subnet, block at Nginx / WAF level:
     ```nginx
     deny 198.51.100.0/24;
     ```
   - Temporarily suspend targeted user accounts from the Admin Console if credentials appear compromised.

---

## 3. Database Maintenance & Backup Procedures

### 3.1 Logical Backup (Daily Automated mysqldump)
Execute a consistent transaction-safe logical backup:
```bash
#!/usr/bin/env bash
BACKUP_DIR="/var/backups/trustpay"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
FILENAME="${BACKUP_DIR}/trustpay_guardian_${TIMESTAMP}.sql.gz"

mkdir -p "${BACKUP_DIR}"

docker exec trustpay-mysql mysqldump \
  --single-transaction \
  --quick \
  --routines \
  --triggers \
  --default-character-set=utf8mb4 \
  -u root -p"${MYSQL_ROOT_PASSWORD}" \
  trustpay_guardian | gzip > "${FILENAME}"

# Verify backup size > 10KB
if [ $(stat -c%s "${FILENAME}") -lt 10240 ]; then
  echo "Backup failed or generated file is suspiciously small!" | mail -s "BACKUP FAILURE" alerts@trustpay.internal
  exit 1
fi

# Rotate backups older than 30 days
find "${BACKUP_DIR}" -type f -name "*.sql.gz" -mtime +30 -delete
```

### 3.2 Point-in-Time Disaster Recovery
To restore from backup into a clean database:
```bash
# 1. Stop backend service to prevent live writes
docker compose stop backend

# 2. Re-create empty target database
docker exec -i trustpay-mysql mysql -u root -p"${MYSQL_ROOT_PASSWORD}" -e "
  DROP DATABASE IF EXISTS trustpay_guardian;
  CREATE DATABASE trustpay_guardian CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
"

# 3. Restore backup data
gunzip < /var/backups/trustpay/trustpay_guardian_YYYYMMDD_HHMMSS.sql.gz | \
  docker exec -i trustpay-mysql mysql -u root -p"${MYSQL_ROOT_PASSWORD}" trustpay_guardian

# 4. Restart backend (Flyway will validate migrations and resume)
docker compose start backend
```

---

## 4. Routine Secrets Rotation

| Secret Key | Rotation Frequency | Impact | Procedure |
|---|---|---|---|
| `APP_SECURITY_JWT_SECRET` | Every 90 days | Active access tokens become invalid; users must re-authenticate | Update env var, execute rolling restart of backend containers |
| `MYSQL_PASSWORD` | Every 180 days | Zero downtime if rotated in MySQL first, then updated in app config | 1. `ALTER USER 'trustpay_user'@'%' IDENTIFIED BY 'new_pass';`<br>2. Update container env var<br>3. Restart backend |
| `DEV_SEED_PASSWORD` | On every production deployment | N/A (disabled in `prod` profile) | Verify dev seeder is disabled in `application-prod.yml` |

---

## 5. Deployment & Rollback Checklist

1. **Pre-Deployment:**
   - Confirm all CI checks passed in GitHub Actions / Jenkins.
   - Verify JaCoCo coverage report >= 80%.
   - Execute backup before applying schema changes.
2. **Zero-Downtime Rollout:**
   - Apply Flyway migrations backward-compatibly (never drop active columns in phase 1).
   - Roll out new backend containers.
   - Roll out new frontend static bundle to Nginx / CDN.
3. **Rollback Strategy:**
   - If backend fails to start: revert container image tag to previous release tag.
   - Flyway migrations are forward-only: if a migration failed, run repair via `scripts/reset-dev-db.sh` or apply compensating rollback SQL.
