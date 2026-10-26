import React from 'react';

export const SecurityHighlightsSection: React.FC = () => {
  return (
    <section id="security" className="py-5 py-lg-6" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
      <div className="container">
        <div className="row align-items-center g-5">
          <div className="col-lg-6">
            <span className="badge bg-danger-subtle text-danger mb-2 px-3 py-1 fw-semibold">
              OWASP & Banking Security
            </span>
            <h2 className="display-6 fw-bold mb-4">Hardened Defense-in-Depth Security Controls</h2>
            <p className="text-body-secondary mb-4">
              Financial platforms require uncompromising security boundaries. TrustPay Guardian applies defense-in-depth across the web tier, application server, and persistence layer.
            </p>

            <div className="d-flex flex-column gap-3">
              <div className="d-flex gap-3 align-items-start">
                <div className="p-2 rounded bg-success-subtle text-success">
                  <i className="bi bi-shield-lock-fill fs-5"></i>
                </div>
                <div>
                  <h3 className="h6 fw-bold mb-1">In-Memory Token Lifecycle & Silent Refresh</h3>
                  <p className="small text-muted mb-0">
                    Access tokens are never stored in localStorage or sessionStorage. 7-day HttpOnly, SameSite=Strict cookies protect against XSS and token exfiltration.
                  </p>
                </div>
              </div>

              <div className="d-flex gap-3 align-items-start">
                <div className="p-2 rounded bg-primary-subtle text-primary">
                  <i className="bi bi-speedometer fs-5"></i>
                </div>
                <div>
                  <h3 className="h6 fw-bold mb-1">Bucket4j Adaptive Rate Limiting</h3>
                  <p className="small text-muted mb-0">
                    Token bucket rate limiting throttles authentication brute-forcing (10 req/min/IP) and transfer flooding (20 req/min/user) with HTTP 429 Retry-After responses.
                  </p>
                </div>
              </div>

              <div className="d-flex gap-3 align-items-start">
                <div className="p-2 rounded bg-warning-subtle text-warning">
                  <i className="bi bi-key-fill fs-5"></i>
                </div>
                <div>
                  <h3 className="h6 fw-bold mb-1">BCrypt Strength 12 & 5-Strike Lockout</h3>
                  <p className="small text-muted mb-0">
                    High-cost adaptive hashing prevents rainbow-table attacks. 5 consecutive invalid passwords trigger an automatic account lockout.
                  </p>
                </div>
              </div>

              <div className="d-flex gap-3 align-items-start">
                <div className="p-2 rounded bg-info-subtle text-info">
                  <i className="bi bi-database-check fs-5"></i>
                </div>
                <div>
                  <h3 className="h6 fw-bold mb-1">Database CHECK Constraints & Atomic State Transitions</h3>
                  <p className="small text-muted mb-0">
                    Relational check constraints guarantee non-negative balances. Ascending row-lock ordering guarantees deadlock-free parallel transaction settlement.
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div className="col-lg-6">
            <div className="card shadow-lg p-4 p-md-5 border-0" style={{ backgroundColor: 'var(--tpg-bg-card)' }}>
              <div className="d-flex justify-content-between align-items-center mb-3">
                <span className="fw-semibold text-primary">Compliance & Architecture Audit</span>
                <span className="badge bg-success">Verified Pass</span>
              </div>

              <ul className="list-group list-group-flush mb-4 small">
                <li className="list-group-item bg-transparent d-flex justify-content-between px-0">
                  <span>IDOR Protection (404 Anti-Enumeration)</span>
                  <strong className="text-success">&check; Enforced</strong>
                </li>
                <li className="list-group-item bg-transparent d-flex justify-content-between px-0">
                  <span>Mass-Assignment Protection (Fixed Role)</span>
                  <strong className="text-success">&check; Enforced</strong>
                </li>
                <li className="list-group-item bg-transparent d-flex justify-content-between px-0">
                  <span>JaCoCo Domain Line Coverage</span>
                  <strong className="text-success">&gt; 80% Gate</strong>
                </li>
                <li className="list-group-item bg-transparent d-flex justify-content-between px-0">
                  <span>Flyway Validate-on-Migrate</span>
                  <strong className="text-success">&check; Active</strong>
                </li>
                <li className="list-group-item bg-transparent d-flex justify-content-between px-0">
                  <span>OpenAPI 3.1 Documentation</span>
                  <strong className="text-success">&check; Swagger UI</strong>
                </li>
              </ul>

              <div className="p-3 rounded bg-body-tertiary border small text-muted">
                <i className="bi bi-info-circle me-1 text-primary"></i>
                Security posture independently audited against OWASP Top 10 standards. Full review recorded in <code>docs/SECURITY_REVIEW.md</code>.
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
