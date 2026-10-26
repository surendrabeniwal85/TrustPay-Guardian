import React from 'react';

interface FeatureItem {
  icon: string;
  title: string;
  description: string;
  badge: string;
}

const FEATURES: FeatureItem[] = [
  {
    icon: 'bi-shield-shaded',
    title: 'Deadlock-Free Row Locks',
    description: 'Deterministic ascending-ID row locking eliminates race conditions and deadlock cycles during bidirectional concurrent transfers.',
    badge: 'Concurrency Guard',
  },
  {
    icon: 'bi-sliders2-vertical',
    title: 'Explainable Rule Engine',
    description: '100% deterministic Strategy-pattern rules (R1 to R7) calculate explainable forensic points rather than inscrutable black-box scores.',
    badge: 'Risk Scoring',
  },
  {
    icon: 'bi-hourglass-split',
    title: 'Dynamic Balance Holds',
    description: 'Suspicious transactions hold sender funds dynamically during analyst triage without crediting the destination or creating ghost money.',
    badge: 'Asset Reservation',
  },
  {
    icon: 'bi-person-badge',
    title: 'Role-Based Authorization',
    description: 'Strict separation of concerns among Customers, Fraud Analysts, and Platform Administrators enforced at the controller and method level.',
    badge: 'Access Control',
  },
  {
    icon: 'bi-journal-check',
    title: 'Immutable Audit Logging',
    description: 'Every privileged admin action, alert decision, and account freeze writes an append-only audit entry stamped with actor and IP address.',
    badge: 'Governance',
  },
  {
    icon: 'bi-incognito',
    title: 'IDOR & Privacy Masking',
    description: 'Anti-enumeration 404 responses for foreign resources, paired with deterministic Indian banking masking for privacy preservation.',
    badge: 'Privacy Defense',
  },
];

export const FeatureCardsSection: React.FC = () => {
  return (
    <section className="py-5 py-lg-6 border-bottom" style={{ backgroundColor: 'var(--tpg-bg-surface)', borderColor: 'var(--tpg-border-color)' }}>
      <div className="container">
        <div className="text-center mb-5">
          <span className="badge bg-info-subtle text-info mb-2 px-3 py-1 fw-semibold">
            Production Engineering Standards
          </span>
          <h2 className="display-6 fw-bold mb-3">Platform Capabilities & Pillars</h2>
          <p className="text-muted mx-auto" style={{ maxWidth: '600px' }}>
            Built on enterprise financial software design patterns with atomic reliability and forensic visibility.
          </p>
        </div>

        <div className="row g-4">
          {FEATURES.map((item) => (
            <div key={item.title} className="col-12 col-md-6 col-lg-4">
              <div className="card h-100 p-4 tpg-card tpg-card-hover border">
                <div className="d-flex align-items-center justify-content-between mb-3">
                  <div className="p-3 rounded-circle bg-primary-subtle text-primary">
                    <i className={`bi ${item.icon} fs-4`}></i>
                  </div>
                  <span className="badge bg-secondary-subtle text-secondary small">{item.badge}</span>
                </div>
                <h3 className="h5 fw-bold mb-2">{item.title}</h3>
                <p className="text-body-secondary small mb-0">{item.description}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};
