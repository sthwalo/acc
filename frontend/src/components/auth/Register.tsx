import { useState, useEffect, useCallback } from 'react';
import { UserPlus, Eye, EyeOff, AlertCircle, CheckCircle, CreditCard } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { serviceRegistry } from '../../services/ServiceRegistry';
import { ApiService } from '../../services/ApiService';
import { PayPalButton } from './PayPalButton';
import type { Plan } from '../../types/api';

interface RegisterProps {
  onSwitchToLogin: () => void;
}

export default function Register({ onSwitchToLogin }: RegisterProps) {
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    confirmPassword: '',
    firstName: '',
    lastName: '',
  });
  const [selectedPlan, setSelectedPlan] = useState<Plan | null>(null);
  const [plans, setPlans] = useState<Plan[]>([]);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [step, setStep] = useState<'form' | 'plan' | 'payment'>('form');

  const { register } = useAuth();

  const loadPlans = useCallback(async () => {
    try {
      const apiService = serviceRegistry.get<ApiService>('apiService');
      const plansData = await apiService.getPlans();
      setPlans(plansData);
    } catch (err) {
      // FAIL FAST: No fallback data allowed - throw error if database empty
      throw new Error(`Failed to load plans from database: ${err instanceof Error ? err.message : 'Unknown error'}`);
    }
  }, []);

  useEffect(() => {
    if (step === 'plan') {
      loadPlans();
    }
  }, [step, loadPlans]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const validateForm = () => {
    if (!formData.email || !formData.password || !formData.firstName || !formData.lastName) {
      setError('All fields are required');
      return false;
    }

    if (formData.password.length < 8) {
      setError('Password must be at least 8 characters long');
      return false;
    }

    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return false;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email)) {
      setError('Please enter a valid email address');
      return false;
    }

    return true;
  };

  const handleFormSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (validateForm()) {
      setStep('plan');
    }
  };

  const handlePlanSelection = (plan: Plan) => {
    setSelectedPlan(plan);
    setStep('payment');
  };

  const handlePaymentSuccess = async (orderId: string, captureId?: string) => {
    setIsLoading(true);
    setError('');

    try {
      await register({
        email: formData.email,
        password: formData.password,
        firstName: formData.firstName,
        lastName: formData.lastName,
        planId: selectedPlan!.id,
        paypalOrderId: orderId,
        paypalCaptureId: captureId
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed. Please try again.');
      setIsLoading(false);
    }
  };

  const handlePaymentError = (errorMessage: string) => {
    setError(errorMessage);
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  if (step === 'plan') {
    return (
      <div className="auth-container">
        <div className="auth-card">
          <div className="auth-header">
            <CheckCircle size={48} className="auth-icon success" />
            <h1>Choose Your Plan</h1>
            <p>Select a plan to complete your registration</p>
          </div>

          <div className="plans-grid">
            {plans.map((plan) => (
              <div key={plan.id} className="plan-card">
                <div className="plan-header">
                  <h3>{plan.name}</h3>
                  <div className="plan-price">
                    <span className="price">{formatCurrency(plan.price_monthly)}</span>
                    <span className="period">/month</span>
                  </div>
                </div>

                <p className="plan-description">{plan.description}</p>

                <ul className="plan-features">
                  {plan.features.map((feature, index) => (
                    <li key={index}>
                      <CheckCircle size={16} className="feature-icon" />
                      {feature}
                    </li>
                  ))}
                </ul>

                <button
                  className="plan-select-button"
                  onClick={() => handlePlanSelection(plan)}
                  disabled={isLoading}
                >
                  {isLoading && selectedPlan?.id === plan.id ? (
                    <>
                      <div className="spinner small"></div>
                      Creating Account...
                    </>
                  ) : (
                    'Select Plan'
                  )}
                </button>
              </div>
            ))}
          </div>

          {error && (
            <div className="error-message">
              <AlertCircle size={20} />
              <span>{error}</span>
            </div>
          )}

          <div className="auth-footer">
            <button
              type="button"
              className="link-button"
              onClick={() => setStep('form')}
              disabled={isLoading}
            >
              ← Back to form
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (step === 'payment') {
    return (
      <div className="auth-container">
        <div className="auth-card">
          <div className="auth-header">
            <CreditCard size={48} className="auth-icon" />
            <h1>Complete Payment</h1>
            <p>Secure payment for your {selectedPlan?.name} plan</p>
          </div>

          <div className="payment-summary">
            <div className="summary-card">
              <h3>Order Summary</h3>
              <div className="summary-row">
                <span>Plan:</span>
                <span>{selectedPlan?.name}</span>
              </div>
              <div className="summary-row">
                <span>Price:</span>
                <span className="price">{formatCurrency(selectedPlan?.price_monthly || 0)}</span>
              </div>
              <div className="summary-row total">
                <span>Total:</span>
                <span className="price">{formatCurrency(selectedPlan?.price_monthly || 0)}</span>
              </div>
            </div>
          </div>

          <div className="payment-section">
            <PayPalButton
              amount={selectedPlan?.price_monthly || 0}
              currency="USD"
              description={`FIN ${selectedPlan?.name} Plan - Monthly Subscription`}
              planId={selectedPlan?.id}
              onSuccess={handlePaymentSuccess}
              onError={handlePaymentError}
              disabled={isLoading}
            />
          </div>

          {error && (
            <div className="error-message">
              <AlertCircle size={20} />
              <span>{error}</span>
            </div>
          )}

          {isLoading && (
            <div className="loading-message">
              <div className="spinner"></div>
              <span>Creating your account...</span>
            </div>
          )}

          <div className="auth-footer">
            <button
              type="button"
              className="link-button"
              onClick={() => setStep('plan')}
              disabled={isLoading}
            >
              ← Back to plans
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <UserPlus size={48} className="auth-icon" />
          <h1>Create Account</h1>
          <p>Join FIN and start managing your finances</p>
        </div>

        <form onSubmit={handleFormSubmit} className="auth-form">
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="firstName">First Name</label>
              <input
                type="text"
                id="firstName"
                name="firstName"
                value={formData.firstName}
                onChange={handleInputChange}
                placeholder="Enter your first name"
                required
                disabled={isLoading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="lastName">Last Name</label>
              <input
                type="text"
                id="lastName"
                name="lastName"
                value={formData.lastName}
                onChange={handleInputChange}
                placeholder="Enter your last name"
                required
                disabled={isLoading}
              />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="email">Email Address</label>
            <input
              type="email"
              id="email"
              name="email"
              value={formData.email}
              onChange={handleInputChange}
              placeholder="Enter your email"
              required
              disabled={isLoading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <div className="password-input">
              <input
                type={showPassword ? 'text' : 'password'}
                id="password"
                name="password"
                value={formData.password}
                onChange={handleInputChange}
                placeholder="Create a password (min 8 characters)"
                required
                disabled={isLoading}
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowPassword(!showPassword)}
                disabled={isLoading}
              >
                {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
              </button>
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword">Confirm Password</label>
            <div className="password-input">
              <input
                type={showConfirmPassword ? 'text' : 'password'}
                id="confirmPassword"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleInputChange}
                placeholder="Confirm your password"
                required
                disabled={isLoading}
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                disabled={isLoading}
              >
                {showConfirmPassword ? <EyeOff size={20} /> : <Eye size={20} />}
              </button>
            </div>
          </div>

          {error && (
            <div className="error-message">
              <AlertCircle size={20} />
              <span>{error}</span>
            </div>
          )}

          <button
            type="submit"
            className="auth-button"
            disabled={isLoading}
          >
            Continue to Plan Selection
          </button>
        </form>

        <div className="auth-footer">
          <p>
            Already have an account?{' '}
            <button
              type="button"
              className="link-button"
              onClick={onSwitchToLogin}
              disabled={isLoading}
            >
              Sign in
            </button>
          </p>
        </div>
      </div>
    </div>
  );
}