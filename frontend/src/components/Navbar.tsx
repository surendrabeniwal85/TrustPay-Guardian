import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { NotificationDropdown } from './NotificationDropdown';

interface NavbarProps {
  theme: 'dark' | 'light';
  toggleTheme: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ theme, toggleTheme }) => {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const getRoleBadge = () => {
    if (!user) return null;
    switch (user.role) {
      case 'ADMIN':
        return <span className="badge bg-danger text-uppercase ms-2">Admin</span>;
      case 'FRAUD_ANALYST':
        return <span className="badge bg-warning text-dark text-uppercase ms-2">Fraud Analyst</span>;
      default:
        return <span className="badge bg-primary text-uppercase ms-2">Customer</span>;
    }
  };

  return (
    <nav className="navbar navbar-expand-lg border-bottom border-secondary border-opacity-25 px-3 py-2 sticky-top bg-body">
      <div className="container-fluid">
        <Link to="/" className="navbar-brand d-flex align-items-center gap-2 fw-bold text-decoration-none">
          <span className="fs-4 text-primary">🛡️</span>
          <span>TrustPay Guardian</span>
          <span className="badge tpg-badge-simulated text-uppercase fw-semibold px-2 py-1">
            Simulated
          </span>
        </Link>

        <button
          className="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#navbarMain"
          aria-controls="navbarMain"
          aria-expanded="false"
          aria-label="Toggle navigation"
        >
          <span className="navbar-toggler-icon"></span>
        </button>

        <div className="collapse navbar-collapse" id="navbarMain">
          <ul className="navbar-nav me-auto mb-2 mb-lg-0">
            {isAuthenticated && user?.role === 'CUSTOMER' && (
              <>
                <li className="nav-item">
                  <Link to="/dashboard" className="nav-link">
                    <i className="bi bi-wallet2 me-1"></i> My Banking
                  </Link>
                </li>
              </>
            )}

            {isAuthenticated && (user?.role === 'FRAUD_ANALYST' || user?.role === 'ADMIN') && (
              <li className="nav-item">
                <Link to="/analyst" className="nav-link">
                  <i className="bi bi-shield-exclamation me-1"></i> Fraud Alert Queue
                </Link>
              </li>
            )}

            {isAuthenticated && user?.role === 'ADMIN' && (
              <li className="nav-item">
                <Link to="/admin" className="nav-link">
                  <i className="bi bi-sliders me-1"></i> Admin Console
                </Link>
              </li>
            )}
          </ul>

          <div className="d-flex align-items-center gap-3">
            {isAuthenticated && <NotificationDropdown />}

            <button
              onClick={toggleTheme}
              className="btn btn-outline-secondary btn-sm rounded-pill px-3"
              aria-label="Toggle theme"
            >
              {theme === 'dark' ? '☀️ Light' : '🌙 Dark'}
            </button>

            {isAuthenticated && user ? (
              <div className="d-flex align-items-center gap-2">
                <div className="d-none d-sm-block text-end">
                  <div className="fw-semibold small">{user.fullName}</div>
                  <div className="text-secondary" style={{ fontSize: '0.75rem' }}>
                    {user.email}
                  </div>
                </div>
                {getRoleBadge()}
                <button
                  onClick={handleLogout}
                  className="btn btn-outline-danger btn-sm rounded-pill ms-2"
                  title="Sign out"
                >
                  <i className="bi bi-box-arrow-right me-1"></i> Logout
                </button>
              </div>
            ) : (
              <div className="d-flex gap-2">
                <Link to="/login" className="btn btn-outline-primary btn-sm rounded-pill px-3">
                  Sign In
                </Link>
                <Link to="/register" className="btn btn-primary btn-sm rounded-pill px-3">
                  Register
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};
