# TrustPay Guardian — Production Deployment & Infrastructure Guide

**Document Version:** 1.0.0  
**Target Environments:** Standalone Linux VM / Docker Compose / Kubernetes Cluster  
**Last Updated:** 2026-10-26  

---

## 1. Deployment Option A: Docker Compose (Recommended for Small/Medium Deployments)

### 1.1 Development / Simulation Mode
```bash
# Clone the codebase and launch full stack
git clone https://github.com/surendrabeniwal85/TrustPay-Guardian.git
cd TrustPay-Guardian
docker compose up --build -d
```
Starts:
- `trustpay-mysql`: MySQL 9.7 LTS on port 3306
- `trustpay-mailhog`: Simulated SMTP on port 1025, Web UI on port 8025
- `trustpay-backend`: Spring Boot API on port 8080
- `trustpay-frontend`: Nginx web server on ports 80 and 3000

### 1.2 Hardened Production Mode (`compose.prod.yml`)
1. Create a secure environment file `.env.prod`:
   ```bash
   MYSQL_ROOT_PASSWORD=$(openssl rand -base64 24)
   MYSQL_USER=trustpay_prod_user
   MYSQL_PASSWORD=$(openssl rand -base64 24)
   MYSQL_DATABASE=trustpay_guardian
   APP_SECURITY_JWT_SECRET=$(openssl rand -hex 64)
   SMTP_HOST=smtp.sendgrid.net
   SMTP_PORT=587
   SMTP_USERNAME=apikey
   SMTP_PASSWORD=your_sendgrid_api_key
   ```
2. Launch production stack:
   ```bash
   docker compose --env-file .env.prod -f compose.prod.yml up --build -d
   ```
   *In production mode, MailHog is excluded, dev seeding is disabled, Actuator is restricted to `/health`, and container resource limits (CPU/Memory) are enforced.*

---

## 2. Deployment Option B: Kubernetes (K8s Manifests)

Below are production-ready manifests for deploying the backend service on Kubernetes:

### 2.1 Backend Deployment & Service (`k8s/backend.yaml`)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: trustpay-backend
  namespace: banking-prod
  labels:
    app: trustpay-backend
spec:
  replicas: 2
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: trustpay-backend
  template:
    metadata:
      labels:
        app: trustpay-backend
    spec:
      containers:
      - name: backend
        image: registry.trustpay.internal/trustpay-guardian-backend:1.0.0
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: 8080
        envFrom:
        - secretRef:
            name: trustpay-secrets
        - configMapRef:
            name: trustpay-config
        resources:
          limits:
            cpu: "2"
            memory: "2048Mi"
          requests:
            cpu: "500m"
            memory: "1024Mi"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 40
          periodSeconds: 15
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: trustpay-backend-svc
  namespace: banking-prod
spec:
  selector:
    app: trustpay-backend
  ports:
  - port: 8080
    targetPort: 8080
  type: ClusterIP
```

---

## 3. Reverse Proxy & SSL Termination (Nginx + Let's Encrypt)

To terminate HTTPS at the host Nginx gateway:
```nginx
server {
    listen 80;
    server_name guardian.trustpay.internal;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name guardian.trustpay.internal;

    ssl_certificate /etc/letsencrypt/live/guardian.trustpay.internal/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/guardian.trustpay.internal/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    location / {
        proxy_pass http://127.0.0.1:80; # Directs to frontend container
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 4. Production Go-Live Verification Checklist

- [ ] Database credentials rotated and not using default passwords.
- [ ] `APP_SECURITY_JWT_SECRET` configured with at least 512 bits of entropy.
- [ ] `SPRING_PROFILES_ACTIVE=prod` verified.
- [ ] Swagger OpenAPI UI verified inaccessible (`404` or `403` returned).
- [ ] Actuator verified exposing ONLY `/actuator/health` with `show-details: never`.
- [ ] Flyway migrations applied successfully (`flyway_schema_history` has no failed rows).
- [ ] Scheduled daily database backup cron job verified.
- [ ] HTTPS certificates valid and HTTP-to-HTTPS redirect enforced.
- [ ] Synthetic data disclaimer displayed prominently on landing page and dashboard.
