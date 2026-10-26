import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useToast } from '../../context/ToastContext';
import { api } from '../../api/axiosClient';

export const ProfileSettingsPage: React.FC = () => {
  const { user } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const { showToast } = useToast();

  // Change Password state
  const [currentPassword, setCurrentPassword] = useState<string>('');
  const [newPassword, setNewPassword] = useState<string>('');
  const [confirmPassword, setConfirmPassword] = useState<string>('');
  const [isChangingPassword, setIsChangingPassword] = useState<boolean>(false);
  const [passwordError, setPasswordError] = useState<string | null>(null);

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordError(null);

    if (newPassword.length < 10) {
      setPasswordError('New password must be at least 10 characters long');
      return;
    }
    if (!/[A-Z]/.test(newPassword) || !/[a-z]/.test(newPassword) || !/[0-9]/.test(newPassword) || !/[^A-Za-z0-9]/.test(newPassword)) {
      setPasswordError('New password must contain uppercase, lowercase, number, and special character');
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordError('New passwords do not match');
      return;
    }

    setIsChangingPassword(true);
    try {
      await api.post('/auth/change-password', {
        currentPassword,
        newPassword,
      });
      showToast('Password changed successfully. Please keep your credentials secure.', 'success');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Password change failed';
      setPasswordError(msg);
      showToast(msg, 'danger');
    } finally {
      setIsChangingPassword(false);
    }
  };

  return (
    <div className="container py-4" style={{ maxWidth: '800px' }}>
      {/* Header */}
      <div className="mb-4">
        <h1 className="h3 fw-bold mb-1">Profile &amp; Settings</h1>
        <p className="text-secondary small mb-0">
          Manage your account credentials, security configuration, and visual theme preferences.
        </p>
      </div>

      {/* Account Profile Card */}
      <div className="card border shadow-sm mb-4" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
        <div className="card-header bg-transparent py-3">
          <h2 className="h5 fw-bold mb-0 d-flex align-items-center gap-2">
            <i className="bi bi-person-badge text-primary"></i>
            <span>Personal Information</span>
          </h2>
        </div>
        <div className="card-body">
          <div className="row g-3">
            <div className="col-md-6">
              <label className="text-secondary small">Full Name</label>
              <div className="fw-semibold fs-6">{user?.fullName || 'Not provided'}</div>
            </div>
            <div className="col-md-6">
              <label className="text-secondary small">Email Address</label>
              <div className="fw-semibold fs-6">{user?.email || 'Not provided'}</div>
            </div>
            <div className="col-md-6">
              <label className="text-secondary small">Registered Mobile</label>
              <div className="fw-semibold fs-6">{user?.phoneNumber || '+91 ••••••••••'}</div>
            </div>
            <div className="col-md-6">
              <label className="text-secondary small">System Role</label>
              <div>
                <span className="badge bg-primary-subtle text-primary border border-primary-subtle">
                  {user?.role || 'CUSTOMER'}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Theme Preference Card */}
      <div className="card border shadow-sm mb-4" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
        <div className="card-header bg-transparent py-3">
          <h2 className="h5 fw-bold mb-0 d-flex align-items-center gap-2">
            <i className="bi bi-palette text-primary"></i>
            <span>Appearance &amp; Theme</span>
          </h2>
        </div>
        <div className="card-body d-flex justify-content-between align-items-center">
          <div>
            <div className="fw-semibold">Color Scheme</div>
            <div className="text-secondary small">
              Currently operating in <strong>{theme === 'dark' ? 'Dark Mode' : 'Light Mode'}</strong>
            </div>
          </div>
          <button
            onClick={toggleTheme}
            className="btn btn-outline-secondary d-flex align-items-center gap-2"
          >
            {theme === 'dark' ? (
              <>
                <i className="bi bi-sun-fill text-warning"></i>
                <span>Switch to Light</span>
              </>
            ) : (
              <>
                <i className="bi bi-moon-fill text-primary"></i>
                <span>Switch to Dark</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Change Password Card */}
      <div className="card border shadow-sm mb-4" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
        <div className="card-header bg-transparent py-3">
          <h2 className="h5 fw-bold mb-0 d-flex align-items-center gap-2">
            <i className="bi bi-key text-primary"></i>
            <span>Change Account Password</span>
          </h2>
        </div>
        <div className="card-body">
          <form onSubmit={handleChangePassword}>
            {passwordError && (
              <div className="alert alert-danger py-2 px-3 small mb-3">
                <i className="bi bi-exclamation-triangle me-1"></i>
                {passwordError}
              </div>
            )}

            <div className="mb-3">
              <label className="form-label small fw-semibold">Current Password</label>
              <input
                type="password"
                className="form-control"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                required
              />
            </div>

            <div className="row g-3 mb-3">
              <div className="col-md-6">
                <label className="form-label small fw-semibold">New Password</label>
                <input
                  type="password"
                  className="form-control"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Min 10 characters"
                  required
                />
              </div>
              <div className="col-md-6">
                <label className="form-label small fw-semibold">Confirm New Password</label>
                <input
                  type="password"
                  className="form-control"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Repeat new password"
                  required
                />
              </div>
            </div>

            <div className="p-2 rounded bg-body-tertiary small text-secondary mb-3">
              Must include uppercase &amp; lowercase letters, at least one digit, and one special character.
            </div>

            <button
              type="submit"
              className="btn btn-primary btn-sm d-flex align-items-center gap-2"
              disabled={isChangingPassword}
            >
              {isChangingPassword && <span className="spinner-border spinner-border-sm" role="status"></span>}
              <span>Update Password</span>
            </button>
          </form>
        </div>
      </div>

      {/* Security Architecture Footnote */}
      <div className="p-3 rounded bg-body-tertiary small text-secondary border">
        <div className="d-flex align-items-center gap-2 fw-semibold text-body mb-1">
          <i className="bi bi-shield-check text-success"></i>
          <span>Zero Client-Storage Policy</span>
        </div>
        TrustPay Guardian strictly avoids saving JWT credentials in <code>localStorage</code> or <code>sessionStorage</code>. Short-lived access tokens exist purely in memory, while sessions are backed by secure HttpOnly SameSite cookies with automatic silent refresh.
      </div>
    </div>
  );
};
