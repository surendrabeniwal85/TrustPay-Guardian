import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../services/api';

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState<string>('john.doe@example.com');
  const [password, setPassword] = useState<string>('Customer@123');
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await login(email, password);
      // Let AuthContext update and redirect
      const savedUser = JSON.parse(localStorage.getItem('tpg_user') || '{}');
      if (savedUser.role === 'ADMIN') {
        navigate('/admin');
      } else if (savedUser.role === 'FRAUD_ANALYST') {
        navigate('/analyst');
      } else {
        navigate('/dashboard');
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const prefill = (e: string, p: string) => {
    setEmail(e);
    setPassword(p);
  };

  return (
    <div className="container py-5">
      <div className="row justify-content-center">
        <div className="col-md-7 col-lg-5">
          <div className="tpg-card p-4 p-md-5">
            <div className="text-center mb-4">
              <span className="fs-1 text-primary">🛡️</span>
              <h3 className="fw-bold mt-2">Sign In</h3>
              <p className="text-secondary small">
                Access your TrustPay Guardian simulated banking account
              </p>
            </div>

            {error && (
              <div className="alert alert-danger small py-2 px-3 mb-3 d-flex align-items-center gap-2">
                <i className="bi bi-exclamation-triangle-fill"></i>
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit}>
              <div className="mb-3">
                <label className="form-label small fw-semibold">Email Address</label>
                <div className="input-group">
                  <span className="input-group-text"><i className="bi bi-envelope"></i></span>
                  <input
                    type="email"
                    className="form-control"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="name@example.com"
                  />
                </div>
              </div>

              <div className="mb-4">
                <label className="form-label small fw-semibold">Password</label>
                <div className="input-group">
                  <span className="input-group-text"><i className="bi bi-lock"></i></span>
                  <input
                    type="password"
                    className="form-control"
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="btn btn-primary w-100 py-2 rounded-pill fw-semibold"
              >
                {loading ? (
                  <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                ) : (
                  <i className="bi bi-box-arrow-in-right me-1"></i>
                )}
                Sign In
              </button>
            </form>

            <div className="mt-4 pt-3 border-top text-center small text-secondary">
              Don't have a demo account?{' '}
              <Link to="/register" className="text-decoration-none fw-semibold">
                Register here
              </Link>
            </div>

            {/* Quick Prefill Section */}
            <div className="mt-4 pt-3 border-top">
              <span className="d-block small text-secondary fw-semibold mb-2 text-center">
                Quick-Fill Demo Credentials:
              </span>
              <div className="d-grid gap-1">
                <button
                  type="button"
                  onClick={() => prefill('john.doe@example.com', 'Customer@123')}
                  className="btn btn-outline-secondary btn-sm text-start"
                >
                  👤 <strong>Customer 1:</strong> john.doe@example.com / Customer@123
                </button>
                <button
                  type="button"
                  onClick={() => prefill('jane.smith@example.com', 'Customer@123')}
                  className="btn btn-outline-secondary btn-sm text-start"
                >
                  👤 <strong>Customer 2:</strong> jane.smith@example.com / Customer@123
                </button>
                <button
                  type="button"
                  onClick={() => prefill('analyst@trustpay.com', 'Analyst@123')}
                  className="btn btn-outline-warning btn-sm text-start text-dark"
                >
                  🔍 <strong>Fraud Analyst:</strong> analyst@trustpay.com / Analyst@123
                </button>
                <button
                  type="button"
                  onClick={() => prefill('admin@trustpay.com', 'Admin@123')}
                  className="btn btn-outline-danger btn-sm text-start"
                >
                  ⚙️ <strong>Administrator:</strong> admin@trustpay.com / Admin@123
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
