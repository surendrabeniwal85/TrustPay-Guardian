import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { getErrorMessage } from '../../api/axiosClient';

const loginSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Please enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export const LoginPage: React.FC = () => {
  const { login, quickLogin, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [showPassword, setShowPassword] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [isLockedOut, setIsLockedOut] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const redirectAfterLogin = (role?: string) => {
    const from = (location.state as { from?: { pathname: string } })?.from?.pathname;
    if (from && from !== '/login' && from !== '/register' && from !== '/403') {
      navigate(from, { replace: true });
      return;
    }

    if (role === 'ADMIN') {
      navigate('/admin/dashboard', { replace: true });
    } else if (role === 'FRAUD_ANALYST') {
      navigate('/analyst/dashboard', { replace: true });
    } else {
      navigate('/app/dashboard', { replace: true });
    }
  };

  const onSubmit = async (data: LoginFormData) => {
    setServerError(null);
    setIsLockedOut(false);
    setIsSubmitting(true);

    try {
      await login(data.email, data.password);
      // Wait for state to settle, then redirect
      redirectAfterLogin(user?.role);
    } catch (err: unknown) {
      const msg = getErrorMessage(err);
      if (msg.toLowerCase().includes('lock') || msg.toLowerCase().includes('too many failed') || msg.toLowerCase().includes('locked')) {
        setIsLockedOut(true);
        setServerError('Account locked due to 5 consecutive failed login attempts. Please wait or contact security support.');
      } else {
        setServerError(msg || 'Invalid email or password. Please verify your credentials.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleQuickLogin = async (roleKey: 'admin' | 'analyst' | 'customer1' | 'customer2') => {
    setServerError(null);
    setIsLockedOut(false);
    setIsSubmitting(true);
    try {
      await quickLogin(roleKey);
      let targetRole = 'CUSTOMER';
      if (roleKey === 'admin') targetRole = 'ADMIN';
      if (roleKey === 'analyst') targetRole = 'FRAUD_ANALYST';
      redirectAfterLogin(targetRole);
    } catch (err: unknown) {
      setServerError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="container min-vh-100 d-flex flex-column justify-content-center py-5">
      <div className="row justify-content-center">
        <div className="col-12 col-md-8 col-lg-5">
          <div className="card shadow-lg border-0 p-4 p-md-5" style={{ backgroundColor: 'var(--tpg-bg-card)' }}>
            <div className="text-center mb-4">
              <Link to="/" className="d-inline-flex align-items-center gap-2 text-decoration-none text-primary fs-3 fw-bold mb-2">
                <i className="bi bi-shield-check"></i> TrustPay Guardian
              </Link>
              <h1 className="h4 fw-bold">Sign In to Your Account</h1>
              <p className="text-muted small">Enter your credentials to access the simulation platform</p>
            </div>

            {/* Lockout Warning Alert */}
            {isLockedOut && (
              <div className="alert alert-danger d-flex align-items-start gap-2 mb-4" role="alert">
                <i className="bi bi-exclamation-triangle-fill fs-5 mt-1"></i>
                <div>
                  <strong>Security Lockout:</strong>
                  <div className="small mt-1">{serverError}</div>
                </div>
              </div>
            )}

            {/* Standard Server Error Alert */}
            {serverError && !isLockedOut && (
              <div className="alert alert-danger py-2 small mb-4" role="alert">
                <i className="bi bi-exclamation-circle me-1"></i> {serverError}
              </div>
            )}

            <form onSubmit={handleSubmit(onSubmit)} noValidate>
              <div className="mb-3">
                <label className="form-label small fw-semibold" htmlFor="emailInput">
                  Email Address
                </label>
                <div className="input-group">
                  <span className="input-group-text bg-body-tertiary border-end-0">
                    <i className="bi bi-envelope text-muted"></i>
                  </span>
                  <input
                    id="emailInput"
                    type="email"
                    className={`form-control border-start-0 ${errors.email ? 'is-invalid' : ''}`}
                    placeholder="name@example.com"
                    autoComplete="email"
                    {...register('email')}
                  />
                  {errors.email && <div className="invalid-feedback">{errors.email.message}</div>}
                </div>
              </div>

              <div className="mb-4">
                <div className="d-flex justify-content-between align-items-center">
                  <label className="form-label small fw-semibold" htmlFor="passwordInput">
                    Password
                  </label>
                </div>
                <div className="input-group">
                  <span className="input-group-text bg-body-tertiary border-end-0">
                    <i className="bi bi-lock text-muted"></i>
                  </span>
                  <input
                    id="passwordInput"
                    type={showPassword ? 'text' : 'password'}
                    className={`form-control border-start-0 border-end-0 ${errors.password ? 'is-invalid' : ''}`}
                    placeholder="Enter password"
                    autoComplete="current-password"
                    {...register('password')}
                  />
                  <button
                    type="button"
                    className="input-group-text bg-body-tertiary border-start-0"
                    onClick={() => setShowPassword(!showPassword)}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    <i className={`bi ${showPassword ? 'bi-eye-slash' : 'bi-eye'}`}></i>
                  </button>
                  {errors.password && <div className="invalid-feedback">{errors.password.message}</div>}
                </div>
              </div>

              <button
                type="submit"
                className="btn btn-primary w-100 py-2 fw-semibold d-flex align-items-center justify-content-center gap-2 mb-3"
                disabled={isSubmitting}
              >
                {isSubmitting ? (
                  <>
                    <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                    <span>Signing In...</span>
                  </>
                ) : (
                  <span>Sign In</span>
                )}
              </button>

              <div className="text-center small text-muted mb-4">
                Don't have an account?{' '}
                <Link to="/register" className="text-decoration-none fw-semibold">
                  Open an Account
                </Link>
              </div>
            </form>

            {/* Quick-Persona Demonstration Switcher */}
            <div className="pt-3 border-top" style={{ borderColor: 'var(--tpg-border-color)' }}>
              <div className="small fw-semibold text-muted text-center mb-2">
                &mdash; Demo 1-Click Persona Sign-In &mdash;
              </div>
              <div className="row g-2">
                <div className="col-6">
                  <button
                    type="button"
                    onClick={() => handleQuickLogin('customer1')}
                    className="btn btn-outline-primary btn-sm w-100 text-truncate"
                    disabled={isSubmitting}
                  >
                    Customer (John)
                  </button>
                </div>
                <div className="col-6">
                  <button
                    type="button"
                    onClick={() => handleQuickLogin('customer2')}
                    className="btn btn-outline-primary btn-sm w-100 text-truncate"
                    disabled={isSubmitting}
                  >
                    Customer (Jane)
                  </button>
                </div>
                <div className="col-6">
                  <button
                    type="button"
                    onClick={() => handleQuickLogin('analyst')}
                    className="btn btn-outline-warning btn-sm w-100 text-truncate"
                    disabled={isSubmitting}
                  >
                    Fraud Analyst
                  </button>
                </div>
                <div className="col-6">
                  <button
                    type="button"
                    onClick={() => handleQuickLogin('admin')}
                    className="btn btn-outline-danger btn-sm w-100 text-truncate"
                    disabled={isSubmitting}
                  >
                    Administrator
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
