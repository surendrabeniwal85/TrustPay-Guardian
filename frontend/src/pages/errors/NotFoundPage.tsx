import React from 'react';
import { Link } from 'react-router-dom';

export const NotFoundPage: React.FC = () => {
  return (
    <div className="container min-vh-100 d-flex align-items-center justify-content-center py-5">
      <div className="card shadow-lg p-5 text-center border-0" style={{ maxWidth: '480px', backgroundColor: 'var(--tpg-bg-card)' }}>
        <div className="mb-4">
          <span className="badge bg-secondary-subtle text-secondary p-3 rounded-circle">
            <i className="bi bi-question-diamond fs-1"></i>
          </span>
        </div>
        <h1 className="h3 fw-bold mb-2">404 &mdash; Page Not Found</h1>
        <p className="text-muted mb-4">
          The banking resource, account statement, or URL you requested does not exist or has been relocated.
        </p>
        <div>
          <Link to="/" className="btn btn-primary px-4">
            <i className="bi bi-house-door me-1"></i> Return Home
          </Link>
        </div>
      </div>
    </div>
  );
};
