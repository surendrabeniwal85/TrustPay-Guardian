import React from 'react';
import { Link } from 'react-router-dom';

export const FinalCtaSection: React.FC = () => {
  return (
    <section className="py-5 py-lg-6 text-center" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
      <div className="container">
        <div
          className="p-5 rounded-4 shadow-lg mx-auto position-relative overflow-hidden"
          style={{
            maxWidth: '960px',
            background: 'linear-gradient(135deg, #1e3a8a 0%, #1d4ed8 50%, #2563eb 100%)',
            color: '#ffffff',
          }}
        >
          <div className="position-relative" style={{ zIndex: 1 }}>
            <span className="badge bg-white text-primary mb-3 px-3 py-1 fw-bold">
              Instant Simulation Sandbox
            </span>
            <h2 className="display-6 fw-bold mb-3 text-white">
              Ready to Test the Fraud Engine?
            </h2>
            <p className="lead text-white text-opacity-90 mb-4 mx-auto" style={{ maxWidth: '600px' }}>
              Create a free simulated account in seconds or use our 1-click persona quick-switcher to test Customer transfers, Analyst triage, and Admin rule tuning.
            </p>

            <div className="d-flex flex-wrap justify-content-center gap-3">
              <Link to="/register" className="btn btn-light btn-lg px-4 py-2 fw-bold text-primary shadow-sm">
                Open Simulated Account
              </Link>
              <Link to="/login" className="btn btn-outline-light btn-lg px-4 py-2 fw-semibold">
                Sign In with Demo Persona
              </Link>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
