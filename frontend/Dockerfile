# ==============================================================================
# TrustPay Guardian — Frontend Multi-Stage Dockerfile (Node 24 + Nginx Alpine)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build Production Bundle
# ------------------------------------------------------------------------------
FROM node:24-alpine AS builder

WORKDIR /app

# Copy dependency manifests
COPY package.json package-lock.json ./

# Clean installation of dependencies
RUN npm ci

# Copy application source tree
COPY . ./

# Build production distribution
RUN npm run build

# ------------------------------------------------------------------------------
# Stage 2: Nginx Web Server with Security Headers
# ------------------------------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

LABEL maintainer="TrustPay Guardian Engineering Team"
LABEL description="TrustPay Guardian Fraud-Aware Digital Banking Platform - Frontend Web Application"
LABEL version="1.0.0"

# Remove default nginx welcome page
RUN rm -rf /usr/share/nginx/html/*

# Copy built distribution from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy production nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

# Health check against dedicated /health route
HEALTHCHECK --interval=15s --timeout=5s --retries=3 \
  CMD wget -q -O - http://localhost/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
