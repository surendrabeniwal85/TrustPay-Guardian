import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';

export const PublicNavbar: React.FC = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();

  const handleDashboardRedirect = () => {
    if (!user) return;
    if (user.role === 'ADMIN') {
      navigate('/admin/dashboard');
    } else if (user.role === 'FRAUD_ANALYST') {
      navigate('/analyst/dashboard');
    } else {
      navigate('/app/dashboard');
    }
  };

  return (
    <nav className="navbar navbar-expand-lg tpg-glass-nav sticky-top">
      <div className="container">
        <Link to="/" className="navbar-brand d-flex align-items-center gap-2 fw-bold fs-5 text-primary">
          <i className="bi bi-shield-check fs-4"></i>
          <span>TrustPay Guardian</span>
        </Link>

        <button
          className="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#publicNavCollapse"
          aria-controls="publicNavCollapse"
          aria-expanded="false"
          aria-label="Toggle navigation"
        >
          <span className="navbar-toggler-icon"></span>
        </button>

        <div className="collapse navbar-collapse" id="publicNavCollapse">
          <ul className="navbar-nav me-auto mb-2 mb-lg-0">
            <li className="nav-item">
              <Link to="/" className="nav-link">Home</Link>
            </li>
            <li className="nav-item">
              <a href="#how-it-works" className="nav-link">How It Works</a>
            </li>
            <li className="nav-item">
              <a href="#security" className="nav-link">Security Controls</a>
            </li>
          </ul>

          <div className="d-flex align-items-center gap-2">
            {/* Theme Toggle */}
            <button
              onClick={toggleTheme}
              className="btn btn-outline-secondary btn-sm rounded-circle p-2"
              title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
              aria-label="Toggle color theme"
            >
              <i className={`bi ${theme === 'dark' ? 'bi-sun-fill text-warning' : 'bi-moon-stars-fill text-primary'}`}></i>
            </button>

            {isAuthenticated ? (
              <div className="d-flex align-items-center gap-2">
                <button onClick={handleDashboardRedirect} className="btn btn-primary btn-sm">
                  <i className="bi bi-speedometer2 me-1"></i> Dashboard
                </button>
                <button onClick={logout} className="btn btn-outline-danger btn-sm">
                  Logout
                </button>
              </div>
            ) : (
              <div className="d-flex align-items-center gap-2">
                <Link to="/login" className="btn btn-outline-primary btn-sm">
                  Sign In
                </Link>
                <Link to="/register" className="btn btn-primary btn-sm">
                  Open Account
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};
