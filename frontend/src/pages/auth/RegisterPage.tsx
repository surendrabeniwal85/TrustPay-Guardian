import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { getErrorMessage } from '../../api/axiosClient';

const registerSchema = z.object({
  fullName: z
    .string()
    .min(2, 'Full name must be at least 2 characters')
    .max(100, 'Full name cannot exceed 100 characters'),
  email: z
    .string()
    .min(1, 'Email is required')
    .email('Please enter a valid email address'),
  phoneNumber: z
    .string()
    .regex(/^[0-9]{10}$/, 'Phone number must be exactly 10 digits'),
  password: z
    .string()
    .min(8, 'Password must be at least 8 characters long')
    .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
    .regex(/[a-z]/, 'Password must contain at least one lowercase letter')
    .regex(/[0-9]/, 'Password must contain at least one number'),
});

type RegisterFormData = z.infer<typeof registerSchema>;

export const RegisterPage: React.FC = () => {
  const { register: registerAuth } = useAuth();
  const navigate = useNavigate();

  const [showPassword, setShowPassword] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    setError,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
  });

  const passwordValue = watch('password', '');

  // Calculate Password Strength (0 - 4 score)
  const calculatePasswordStrength = (pwd: string): { score: number; label: string; color: string; width: string } => {
    if (!pwd) return { score: 0, label: '', color: 'bg-secondary', width: '0%' };
    let score = 0;
    if (pwd.length >= 8) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/[a-z]/.test(pwd)) score++;
    if (/[0-9]/.test(pwd)) score++;
    if (/[^A-Za-z0-9]/.test(pwd)) score++;

    switch (score) {
      case 1:
        return { score: 1, label: 'Weak', color: 'bg-danger', width: '25%' };
      case 2:
        return { score: 2, label: 'Fair', color: 'bg-warning', width: '50%' };
      case 3:
      case 4:
        return { score: 3, label: 'Good', color: 'bg-info', width: '75%' };
      case 5:
        return { score: 4, label: 'Strong', color: 'bg-success', width: '100%' };
      default:
        return { score: 1, label: 'Weak', color: 'bg-danger', width: '25%' };
    }
  };

  const strength = calculatePasswordStrength(passwordValue);

  const onSubmit = async (data: RegisterFormData) => {
    setServerError(null);
    setIsSubmitting(true);

    try {
      await registerAuth(data.fullName, data.email, data.phoneNumber, data.password);
      navigate('/app/dashboard', { replace: true });
    } catch (err: unknown) {
      const msg = getErrorMessage(err);
      if (msg.toLowerCase().includes('email')) {
        setError('email', { message: 'An account with this email address already exists' });
      } else if (msg.toLowerCase().includes('phone')) {
        setError('phoneNumber', { message: 'An account with this phone number already exists' });
      } else {
        setServerError(msg);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="container min-vh-100 d-flex flex-column justify-content-center py-5">
      <div className="row justify-content-center">
        <div className="col-12 col-md-8 col-lg-6">
          <div className="card shadow-lg border-0 p-4 p-md-5" style={{ backgroundColor: 'var(--tpg-bg-card)' }}>
            <div className="text-center mb-4">
              <Link to="/" className="d-inline-flex align-items-center gap-2 text-decoration-none text-primary fs-3 fw-bold mb-2">
                <i className="bi bi-shield-check"></i> TrustPay Guardian
              </Link>
              <h1 className="h4 fw-bold">Open a Simulated Account</h1>
              <p className="text-muted small">Instant simulated savings account with ₹50,000 demo opening balance</p>
            </div>

            {serverError && (
              <div className="alert alert-danger py-2 small mb-4" role="alert">
                <i className="bi bi-exclamation-circle me-1"></i> {serverError}
              </div>
            )}

            <form onSubmit={handleSubmit(onSubmit)} noValidate>
              <div className="mb-3">
                <label className="form-label small fw-semibold" htmlFor="fullNameInput">
                  Full Legal Name
                </label>
                <input
                  id="fullNameInput"
                  type="text"
                  className={`form-control ${errors.fullName ? 'is-invalid' : ''}`}
                  placeholder="e.g. Rahul Sharma"
                  autoComplete="name"
                  {...register('fullName')}
                />
                {errors.fullName && <div className="invalid-feedback">{errors.fullName.message}</div>}
              </div>

              <div className="mb-3">
                <label className="form-label small fw-semibold" htmlFor="emailInput">
                  Email Address
                </label>
                <input
                  id="emailInput"
                  type="email"
                  className={`form-control ${errors.email ? 'is-invalid' : ''}`}
                  placeholder="name@example.com"
                  autoComplete="email"
                  {...register('email')}
                />
                {errors.email && <div className="invalid-feedback">{errors.email.message}</div>}
              </div>

              <div className="mb-3">
                <label className="form-label small fw-semibold" htmlFor="phoneInput">
                  Phone Number (10 Digits)
                </label>
                <input
                  id="phoneInput"
                  type="tel"
                  className={`form-control ${errors.phoneNumber ? 'is-invalid' : ''}`}
                  placeholder="9876543210"
                  maxLength={10}
                  autoComplete="tel"
                  {...register('phoneNumber')}
                />
                {errors.phoneNumber && <div className="invalid-feedback">{errors.phoneNumber.message}</div>}
              </div>

              <div className="mb-4">
                <label className="form-label small fw-semibold" htmlFor="passwordInput">
                  Password
                </label>
                <div className="input-group mb-1">
                  <input
                    id="passwordInput"
                    type={showPassword ? 'text' : 'password'}
                    className={`form-control ${errors.password ? 'is-invalid' : ''}`}
                    placeholder="Minimum 8 characters"
                    autoComplete="new-password"
                    {...register('password')}
                  />
                  <button
                    type="button"
                    className="input-group-text bg-body-tertiary"
                    onClick={() => setShowPassword(!showPassword)}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    <i className={`bi ${showPassword ? 'bi-eye-slash' : 'bi-eye'}`}></i>
                  </button>
                  {errors.password && <div className="invalid-feedback">{errors.password.message}</div>}
                </div>

                {/* Password Strength Meter */}
                {passwordValue && (
                  <div className="mt-2">
                    <div className="d-flex justify-content-between align-items-center small text-muted mb-1">
                      <span>Password Strength:</span>
                      <strong className={`text-${strength.color.replace('bg-', '')}`}>{strength.label}</strong>
                    </div>
                    <div className="progress" style={{ height: '4px' }}>
                      <div
                        className={`progress-bar ${strength.color}`}
                        role="progressbar"
                        style={{ width: strength.width }}
                        aria-valuenow={strength.score * 25}
                        aria-valuemin={0}
                        aria-valuemax={100}
                      ></div>
                    </div>
                  </div>
                )}
              </div>

              <button
                type="submit"
                className="btn btn-primary w-100 py-2 fw-semibold d-flex align-items-center justify-content-center gap-2 mb-3"
                disabled={isSubmitting}
              >
                {isSubmitting ? (
                  <>
                    <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                    <span>Creating Account...</span>
                  </>
                ) : (
                  <span>Complete Registration</span>
                )}
              </button>

              <div className="text-center small text-muted">
                Already registered?{' '}
                <Link to="/login" className="text-decoration-none fw-semibold">
                  Sign In
                </Link>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};
