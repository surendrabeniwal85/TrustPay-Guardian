import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export const ForbiddenPage: React.FC = () => {
  const { user, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const handleReturn = () => {
    if (!isAuthenticated || !user) {
      navigate('/login');
    } else if (user.role === 'ADMIN') {
      navigate('/admin/dashboard');
    } else if (user.role === 'FRAUD_ANALYST') {
      navigate('/analyst/dashboard');
    } else {
      navigate('/app/dashboard');
    }
  };

  return (
    <div className="container min-vh-100 d-flex align-items-center justify-content-center py-5">
      <div className="card shadow-lg p-5 text-center border-0" style={{ maxWidth: '480px', backgroundColor: 'var(--tpg-bg-card)' }}>
        <div className="mb-4">
          <span className="badge bg-danger-subtle text-danger p-3 rounded-circle">
            <i className="bi bi-shield-slash fs-1"></i>
          </span>
        </div>
        <h1 className="h3 fw-bold mb-2">403 &mdash; Access Restricted</h1>
        <p className="text-muted mb-4">
          You do not have the required permissions or role privileges to view this console or resource.
        </p>
        <div className="d-flex justify-content-center gap-2">
          <button onClick={handleReturn} className="btn btn-primary">
            <i className="bi bi-arrow-left me-1"></i> Return to Safe Area
          </button>
          <Link to="/" className="btn btn-outline-secondary">
            Home
          </Link>
        </div>
      </div>
    </div>
  );
};
