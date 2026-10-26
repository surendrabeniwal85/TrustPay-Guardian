import React, { useState } from 'react';
import { Link, useLocation, useNavigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { NotificationDropdown } from '../NotificationDropdown';
import { Footer } from './Footer';

interface NavItem {
  label: string;
  path: string;
  icon: string;
}

export const AppShell: React.FC = () => {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const location = useLocation();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const getNavItems = (): NavItem[] => {
    if (!user) return [];

    if (user.role === 'ADMIN') {
      return [
        { label: 'Admin Dashboard', path: '/admin/dashboard', icon: 'bi-speedometer2' },
        { label: 'User Governance', path: '/admin/users', icon: 'bi-people' },
        { label: 'Fraud Rules', path: '/admin/fraud-rules', icon: 'bi-sliders' },
        { label: 'System Settings', path: '/admin/settings', icon: 'bi-gear' },
        { label: 'Audit Logs', path: '/admin/audit-logs', icon: 'bi-journal-text' },
      ];
    }

    if (user.role === 'FRAUD_ANALYST') {
      return [
        { label: 'Analyst Dashboard', path: '/analyst/dashboard', icon: 'bi-shield-check' },
        { label: 'Alert Queue', path: '/analyst/alerts', icon: 'bi-exclamation-triangle' },
      ];
    }

    // Default: Customer
    return [
      { label: 'My Dashboard', path: '/app/dashboard', icon: 'bi-house' },
      { label: 'Accounts & Statement', path: '/app/accounts', icon: 'bi-wallet2' },
      { label: 'Send Money', path: '/app/transfer', icon: 'bi-arrow-left-right' },
      { label: 'Beneficiaries', path: '/app/beneficiaries', icon: 'bi-person-lines-fill' },
      { label: 'Transactions', path: '/app/history', icon: 'bi-clock-history' },
    ];
  };

  const navItems = getNavItems();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const getRoleBadge = () => {
    switch (user?.role) {
      case 'ADMIN':
        return <span className="badge bg-danger">Administrator</span>;
      case 'FRAUD_ANALYST':
        return <span className="badge bg-warning text-dark">Fraud Analyst</span>;
      default:
        return <span className="badge bg-primary">Customer</span>;
    }
  };

  return (
    <div className="tpg-layout">
      {/* Sidebar */}
      <aside className={`tpg-sidebar ${mobileMenuOpen ? 'open' : ''}`}>
        <div className="p-3 border-bottom d-flex align-items-center justify-content-between" style={{ borderColor: 'var(--tpg-border-color)' }}>
          <Link to="/" className="d-flex align-items-center gap-2 text-decoration-none fw-bold fs-5 text-primary">
            <i className="bi bi-shield-check fs-4"></i>
            <span>TrustPay</span>
          </Link>
          <button
            className="btn btn-sm btn-link text-secondary d-md-none p-0"
            onClick={() => setMobileMenuOpen(false)}
            aria-label="Close sidebar"
          >
            <i className="bi bi-x-lg fs-5"></i>
          </button>
        </div>

        {/* Navigation items */}
        <div className="py-3 flex-grow-1 overflow-auto">
          <ul className="nav nav-pills flex-column px-2 gap-1">
            {navItems.map((item) => {
              const isActive = location.pathname === item.path;
              return (
                <li key={item.path} className="nav-item">
                  <Link
                    to={item.path}
                    className={`nav-link d-flex align-items-center gap-2 py-2 px-3 rounded ${
                      isActive ? 'active bg-primary text-white' : 'text-body-secondary'
                    }`}
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    <i className={`bi ${item.icon} fs-5`}></i>
                    <span>{item.label}</span>
                  </Link>
                </li>
              );
            })}
          </ul>
        </div>

        {/* Sidebar Footer info */}
        <div className="p-3 border-top small text-muted" style={{ borderColor: 'var(--tpg-border-color)' }}>
          <div className="d-flex align-items-center gap-2 mb-2">
            <i className="bi bi-shield-lock text-success"></i>
            <span>Fraud Shield Active</span>
          </div>
          <div className="badge bg-secondary-subtle text-secondary w-100 py-1">
            Simulated Ledger
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="tpg-main-content">
        {/* Top Navbar */}
        <header className="navbar navbar-expand tpg-glass-nav px-3 py-2">
          <button
            className="btn btn-outline-secondary btn-sm d-md-none me-2"
            onClick={() => setMobileMenuOpen(true)}
            aria-label="Open sidebar"
          >
            <i className="bi bi-list fs-5"></i>
          </button>

          <div className="d-flex align-items-center gap-2">
            {getRoleBadge()}
            <span className="d-none d-sm-inline text-muted small">| Logged in as <strong className="text-body">{user?.fullName}</strong></span>
          </div>

          <div className="ms-auto d-flex align-items-center gap-2">
            {/* Notification Bell */}
            <NotificationDropdown />

            {/* Theme Toggle */}
            <button
              onClick={toggleTheme}
              className="btn btn-outline-secondary btn-sm rounded-circle p-2"
              title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
              aria-label="Toggle color theme"
            >
              <i className={`bi ${theme === 'dark' ? 'bi-sun-fill text-warning' : 'bi-moon-stars-fill text-primary'}`}></i>
            </button>

            {/* User Profile / Logout */}
            <div className="dropdown">
              <button
                className="btn btn-outline-secondary btn-sm dropdown-toggle d-flex align-items-center gap-1"
                type="button"
                data-bs-toggle="dropdown"
                aria-expanded="false"
              >
                <i className="bi bi-person-circle"></i>
                <span className="d-none d-md-inline ms-1">{user?.fullName?.split(' ')[0]}</span>
              </button>
              <ul className="dropdown-menu dropdown-menu-end shadow">
                <li className="dropdown-header">
                  <div className="fw-semibold">{user?.fullName}</div>
                  <div className="small text-muted">{user?.email}</div>
                </li>
                <li><hr className="dropdown-divider" /></li>
                <li>
                  <button className="dropdown-item text-danger d-flex align-items-center gap-2" onClick={handleLogout}>
                    <i className="bi bi-box-arrow-right"></i> Sign Out
                  </button>
                </li>
              </ul>
            </div>
          </div>
        </header>

        {/* Page Content View */}
        <main className="flex-grow-1 p-3 p-md-4">
          <Outlet />
        </main>

        {/* App Footer */}
        <Footer />
      </div>
    </div>
  );
};
