import React from 'react';

export const Footer: React.FC = () => {
  return (
    <footer className="py-4 border-top mt-auto" style={{ backgroundColor: 'var(--tpg-bg-card)', borderColor: 'var(--tpg-border-color)' }}>
      <div className="container d-flex flex-column flex-md-row justify-content-between align-items-center text-muted small">
        <div className="mb-2 mb-md-0">
          <span className="fw-semibold text-primary">TrustPay Guardian</span> &copy; {new Date().getFullYear()} &mdash; Educational Simulation Platform
        </div>
        <div>
          <span className="badge bg-warning text-dark me-2">Educational Simulation</span>
          <span>No real money &bull; Synthetic data &bull; Not PCI-DSS certified</span>
        </div>
      </div>
    </footer>
  );
};
