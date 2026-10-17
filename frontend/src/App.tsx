import React, { useState, useEffect } from 'react';
import axios from 'axios';

interface HealthData {
  status: string;
  application: string;
  service: string;
  version: string;
  profile: string;
  simulationMode: boolean;
  timestamp: string;
}

export const App: React.FC = () => {
  const [theme, setTheme] = useState<'dark' | 'light'>(() => {
    return (localStorage.getItem('tpg-theme') as 'dark' | 'light') || 'dark';
  });
  const [health, setHealth] = useState<HealthData | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    document.documentElement.setAttribute('data-bs-theme', theme);
    localStorage.setItem('tpg-theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'));
  };

  const checkHealth = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get<HealthData>('/api/v1/health');
      setHealth(response.data);
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        setError(err.message || 'Failed to connect to backend.');
      } else {
        setError('An unexpected error occurred.');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    checkHealth();
  }, []);

  return (
    <div className="d-flex flex-column min-vh-100">
      {/* Navbar Header */}
      <nav className="navbar navbar-expand-lg border-bottom border-secondary border-opacity-25 px-4 py-3">
        <div className="container-fluid">
          <div className="d-flex align-items-center gap-2">
            <span className="fs-4 text-primary">🛡️</span>
            <span className="navbar-brand fw-bold mb-0">TrustPay Guardian</span>
            <span className="badge tpg-badge-simulated text-uppercase fw-semibold px-2 py-1">
              Simulation Only
            </span>
          </div>

          <div className="d-flex align-items-center gap-3">
            <button
              onClick={toggleTheme}
              className="btn btn-outline-secondary btn-sm rounded-pill px-3"
              aria-label="Toggle dark/light theme"
            >
              {theme === 'dark' ? '☀️ Light' : '🌙 Dark'}
            </button>
          </div>
        </div>
      </nav>

      {/* Main Content Area */}
      <main className="container flex-grow-1 py-5">
        <div className="text-center mb-5">
          <h1 className="display-5 fw-bold mb-3">
            TrustPay Guardian: Fraud-Aware Digital Banking Platform
          </h1>
          <p className="lead text-secondary max-w-75 mx-auto">
            Educational, portfolio-grade digital banking simulation with layered Java architecture,
            MySQL persistence, explainable fraud-risk rules, and role-based access control.
          </p>
          <div className="alert alert-warning d-inline-block text-start border-warning border-opacity-50 py-2 px-3 small">
            ⚠️ <strong>Notice:</strong> This platform is a software engineering educational simulation.
            No real banking connectivity, money transfers, or financial data are processed.
          </div>
        </div>

        {/* Phase 1 Toolchain Status Grid */}
        <div className="row g-4 justify-content-center">
          {/* Backend Health Card */}
          <div className="col-md-6 col-lg-5">
            <div className="tpg-card p-4 h-100 d-flex flex-column justify-content-between">
              <div>
                <div className="d-flex justify-content-between align-items-center mb-3">
                  <h5 className="fw-semibold mb-0">Backend API Health</h5>
                  {loading && <span className="spinner-border spinner-border-sm text-primary" role="status" />}
                  {!loading && health && <span className="badge bg-success-subtle text-success">UP</span>}
                  {!loading && error && <span className="badge bg-danger-subtle text-danger">OFFLINE</span>}
                </div>

                {loading && <p className="text-secondary small">Connecting to Spring Boot REST API...</p>}

                {!loading && health && (
                  <div className="small">
                    <div className="mb-2"><strong>Application:</strong> {health.application}</div>
                    <div className="mb-2"><strong>Service:</strong> <code>{health.service}</code></div>
                    <div className="mb-2"><strong>Version:</strong> {health.version}</div>
                    <div className="mb-2"><strong>Active Profile:</strong> <code>{health.profile}</code></div>
                    <div className="mb-2"><strong>Timestamp:</strong> {health.timestamp}</div>
                  </div>
                )}

                {!loading && error && (
                  <div className="alert alert-danger py-2 small mb-0">
                    <div><strong>Connection Error:</strong> {error}</div>
                    <div className="text-secondary mt-1">Make sure the Spring Boot backend is running on <code>localhost:8080</code>.</div>
                  </div>
                )}
              </div>

              <div className="mt-4 pt-3 border-top border-secondary border-opacity-25 d-flex justify-content-between align-items-center">
                <span className="small text-secondary">Endpoint: <code>/api/v1/health</code></span>
                <button
                  onClick={checkHealth}
                  disabled={loading}
                  className="btn btn-sm btn-primary px-3"
                >
                  Refresh Health
                </button>
              </div>
            </div>
          </div>

          {/* Architecture Status Card */}
          <div className="col-md-6 col-lg-5">
            <div className="tpg-card p-4 h-100 d-flex flex-column justify-content-between">
              <div>
                <h5 className="fw-semibold mb-3">Architecture & Environment</h5>
                <ul className="list-unstyled small mb-0">
                  <li className="mb-2 d-flex justify-content-between">
                    <span className="text-secondary">Architecture:</span>
                    <span className="fw-semibold">Modular Monolith</span>
                  </li>
                  <li className="mb-2 d-flex justify-content-between">
                    <span className="text-secondary">Java Runtime:</span>
                    <span>Java 25 LTS</span>
                  </li>
                  <li className="mb-2 d-flex justify-content-between">
                    <span className="text-secondary">Backend Framework:</span>
                    <span>Spring Boot 4.1.1 GA</span>
                  </li>
                  <li className="mb-2 d-flex justify-content-between">
                    <span className="text-secondary">Relational Database:</span>
                    <span>MySQL 8.4 LTS (Port 3306)</span>
                  </li>
                  <li className="mb-2 d-flex justify-content-between">
                    <span className="text-secondary">Frontend Toolchain:</span>
                    <span>React 19 + TypeScript + Vite</span>
                  </li>
                  <li className="d-flex justify-content-between">
                    <span className="text-secondary">Styling:</span>
                    <span>Bootstrap 5.3 + Theme Tokens</span>
                  </li>
                </ul>
              </div>

              <div className="mt-4 pt-3 border-top border-secondary border-opacity-25 small text-secondary">
                Phase 1 Skeleton & Toolchain verified and operational.
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="border-top border-secondary border-opacity-25 py-3 text-center small text-secondary">
        TrustPay Guardian: Fraud-Aware Digital Banking Platform &bull; Phase 1 Skeleton
      </footer>
    </div>
  );
};

export default App;
