import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { useAuth } from '../context/AuthContext';

interface HealthData {
  status: string;
  application: string;
  version: string;
  simulationMode: boolean;
  timestamp: string;
}

export const LandingPage: React.FC = () => {
  const [health, setHealth] = useState<HealthData | null>(null);
  const { isAuthenticated, user, quickLogin } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    api.get<HealthData>('/health')
      .then((res) => setHealth(res.data))
      .catch(() => setHealth(null));
  }, []);

  const handlePersona = async (role: 'customer1' | 'customer2' | 'analyst' | 'admin') => {
    await quickLogin(role);
    if (role === 'admin') navigate('/admin');
    else if (role === 'analyst') navigate('/analyst');
    else navigate('/dashboard');
  };

  return (
    <div className="container py-5">
      {/* Hero Section */}
      <div className="text-center mb-5">
        <div className="d-inline-flex align-items-center gap-2 mb-3">
          <span className="fs-1">🛡️</span>
          <span className="badge tpg-badge-simulated text-uppercase px-3 py-2 fs-6">
            Academic Portfolio & Simulation Platform
          </span>
        </div>
        <h1 className="display-4 fw-bold mb-3">
          TrustPay Guardian
        </h1>
        <p className="fs-5 text-secondary max-w-75 mx-auto" style={{ maxWidth: '820px' }}>
          A fraud-aware digital banking platform built with Java 25, Spring Boot 4, MySQL 8.4,
          deterministic fraud scoring, concurrency-safe transfers, and analyst review workflows.
        </p>

        <div className="alert alert-warning d-inline-block text-start border-warning border-opacity-50 py-2 px-4 small my-3">
          ⚠️ <strong>Educational Simulation Disclaimer:</strong> This platform is designed solely for
          software engineering demonstration. All account numbers, balances, transactions, and fraud alerts
          are strictly simulated.
        </div>

        <div className="d-flex justify-content-center gap-3 mt-3">
          {isAuthenticated ? (
            <Link
              to={user?.role === 'ADMIN' ? '/admin' : user?.role === 'FRAUD_ANALYST' ? '/analyst' : '/dashboard'}
              className="btn btn-primary btn-lg rounded-pill px-4"
            >
              Go to Your Dashboard ({user?.role}) →
            </Link>
          ) : (
            <>
              <Link to="/login" className="btn btn-primary btn-lg rounded-pill px-4">
                Sign In
              </Link>
              <Link to="/register" className="btn btn-outline-primary btn-lg rounded-pill px-4">
                Open Demo Account
              </Link>
            </>
          )}
        </div>
      </div>

      {/* Live System Status */}
      <div className="row g-4 mb-5 justify-content-center">
        <div className="col-md-4">
          <div className="tpg-card p-4 h-100">
            <div className="d-flex align-items-center gap-3 mb-2">
              <i className="bi bi-hdd-network text-success fs-3"></i>
              <div>
                <h6 className="mb-0 fw-semibold">Backend API Engine</h6>
                <small className="text-secondary">Java 25 & Spring Boot 4.1.1</small>
              </div>
            </div>
            <div className="mt-3">
              <span className={`badge ${health ? 'bg-success' : 'bg-secondary'}`}>
                {health ? 'ONLINE' : 'CONNECTING...'}
              </span>
              <span className="ms-2 small text-secondary">
                {health?.simulationMode ? 'Simulation Active' : ''}
              </span>
            </div>
          </div>
        </div>

        <div className="col-md-4">
          <div className="tpg-card p-4 h-100">
            <div className="d-flex align-items-center gap-3 mb-2">
              <i className="bi bi-database-check text-primary fs-3"></i>
              <div>
                <h6 className="mb-0 fw-semibold">Database Persistence</h6>
                <small className="text-secondary">MySQL 8.4.11 & Flyway V1–V6</small>
              </div>
            </div>
            <div className="mt-3">
              <span className="badge bg-primary">11 TABLES VALIDATED</span>
              <span className="ms-2 small text-secondary">ddl-auto: validate</span>
            </div>
          </div>
        </div>

        <div className="col-md-4">
          <div className="tpg-card p-4 h-100">
            <div className="d-flex align-items-center gap-3 mb-2">
              <i className="bi bi-shield-lock text-warning fs-3"></i>
              <div>
                <h6 className="mb-0 fw-semibold">Fraud Risk Engine</h6>
                <small className="text-secondary">R1–R7 Strategy Pattern</small>
              </div>
            </div>
            <div className="mt-3">
              <span className="badge bg-warning text-dark">7 ACTIVE RULES</span>
              <span className="ms-2 small text-secondary">Thresholds: 40 / 70</span>
            </div>
          </div>
        </div>
      </div>

      {/* Architecture Highlights Grid */}
      <h3 className="fw-bold mb-4 text-center">Core Architectural Guarantees</h3>
      <div className="row g-4 mb-5">
        <div className="col-md-6 col-lg-4">
          <div className="tpg-card p-4 h-100">
            <div className="fs-2 text-primary mb-2">🔒</div>
            <h5 className="fw-semibold">Ascending-ID Row Locking</h5>
            <p className="text-secondary small">
              Prevents deadlocks and race conditions during simultaneous transfers by always locking
              <code>min(src, dst)</code> followed by <code>max(src, dst)</code> with pessimistic write locks.
            </p>
          </div>
        </div>

        <div className="col-md-6 col-lg-4">
          <div className="tpg-card p-4 h-100">
            <div className="fs-2 text-warning mb-2">⚖️</div>
            <h5 className="fw-semibold">Available vs Ledger Balance</h5>
            <p className="text-secondary small">
              Held transfers in review dynamically reduce available balance without modifying ledger balances.
              Analyst approval posts debits/credits; analyst blocking restores available funds.
            </p>
          </div>
        </div>

        <div className="col-md-6 col-lg-4">
          <div className="tpg-card p-4 h-100">
            <div className="fs-2 text-success mb-2">🧠</div>
            <h5 className="fw-semibold">Deterministic Fraud Scoring</h5>
            <p className="text-secondary small">
              Composite risk scores (0–100) evaluated across 7 independent rules (amount, velocity, unusual hours,
              new beneficiaries, sudden spikes, limit proximity, recent failed logins).
            </p>
          </div>
        </div>

        <div className="col-md-6 col-lg-4">
          <div className="tpg-card p-4 h-100">
            <div className="fs-2 text-info mb-2">🔁</div>
            <h5 className="fw-semibold">Idempotency-Key Protection</h5>
            <p className="text-secondary small">
              Client requests include unique UUIDs. Retried requests return cached transaction responses
              without creating duplicate debits or secondary transfers.
            </p>
          </div>
        </div>

        <div className="col-md-6 col-lg-4">
          <div className="tpg-card p-4 h-100">
            <div className="fs-2 text-danger mb-2">🛡️</div>
            <h5 className="fw-semibold">Brute-Force & Theft Defense</h5>
            <p className="text-secondary small">
              Accounts lock for 15 minutes after 5 consecutive failed logins. Refresh tokens rotate on every
              use; reuse of a revoked token triggers instant global session revocation.
            </p>
          </div>
        </div>

        <div className="col-md-6 col-lg-4">
          <div className="tpg-card p-4 h-100">
            <div className="fs-2 text-primary mb-2">📜</div>
            <h5 className="fw-semibold">Immutable Audit Trail</h5>
            <p className="text-secondary small">
              Administrative actions, analyst decisions, and security alerts write to append-only audit logs
              isolated with <code>REQUIRES_NEW</code> transactions and schema privilege controls.
            </p>
          </div>
        </div>
      </div>

      {/* Interactive Personas Card */}
      <div className="tpg-card p-4 p-md-5 text-center bg-body-tertiary">
        <h4 className="fw-bold mb-2">Interactive Evaluation Personas</h4>
        <p className="text-secondary mb-4">
          Click any persona below to authenticate instantly and explore the system in that role:
        </p>
        <div className="d-flex flex-wrap justify-content-center gap-3">
          <button
            onClick={() => handlePersona('customer1')}
            className="btn btn-outline-primary rounded-pill px-4 py-2"
          >
            👤 <strong>John Doe</strong> (Customer • ₹50k balance)
          </button>
          <button
            onClick={() => handlePersona('customer2')}
            className="btn btn-outline-primary rounded-pill px-4 py-2"
          >
            👤 <strong>Jane Smith</strong> (Customer • ₹30k balance)
          </button>
          <button
            onClick={() => handlePersona('analyst')}
            className="btn btn-outline-warning rounded-pill px-4 py-2"
          >
            🔍 <strong>Fraud Analyst</strong> (Review & Release alerts)
          </button>
          <button
            onClick={() => handlePersona('admin')}
            className="btn btn-outline-danger rounded-pill px-4 py-2"
          >
            ⚙️ <strong>Administrator</strong> (Rules, Settings & Audit)
          </button>
        </div>
      </div>
    </div>
  );
};
