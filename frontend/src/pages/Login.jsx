import { useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { API_URL, saveAuthSession } from '../services/auth';
import {
  getLoginDestination,
  getLoginFailureMessage,
  getLoginInitialForm,
} from '../services/authFlow';
import { validateEmail } from '../services/validation';

function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const [formState, setFormState] = useState(() => ({
    locationKey: location.key,
    values: getLoginInitialForm(location.state),
  }));
  const formData = formState.locationKey === location.key
    ? formState.values
    : getLoginInitialForm(location.state);
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [message, setMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const submitting = useRef(false);

  const updateFormData = (updates) => {
    setFormState((previous) => ({
      locationKey: location.key,
      values: {
        ...(previous.locationKey === location.key
          ? previous.values
          : getLoginInitialForm(location.state)),
        ...updates,
      },
    }));
  };

  const validate = (field, value) => {
    if (field === 'email') return validateEmail(value);
    if (field === 'password' && !value) return 'Password is required.';
    return '';
  };

  const handleChange = (event) => {
    const { name, value } = event.target;
    updateFormData({ [name]: value });
    setMessage('');
    if (touched[name]) {
      setErrors((previous) => ({ ...previous, [name]: validate(name, value) }));
    }
  };

  const handleBlur = (event) => {
    const { name, value } = event.target;
    const normalizedValue = name === 'email' ? value.trim() : value;
    updateFormData({ [name]: normalizedValue });
    setTouched((previous) => ({ ...previous, [name]: true }));
    setErrors((previous) => ({ ...previous, [name]: validate(name, normalizedValue) }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (submitting.current) return;
    setMessage('');
    const normalizedEmail = formData.email.trim();
    const validationErrors = {
      email: validate('email', normalizedEmail),
      password: validate('password', formData.password),
    };
    updateFormData({ email: normalizedEmail });
    setTouched({ email: true, password: true });
    setErrors(validationErrors);
    if (Object.values(validationErrors).some(Boolean)) return;

    submitting.current = true;
    setIsSubmitting(true);
    try {
      const response = await fetch(`${API_URL}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: normalizedEmail, password: formData.password }),
      });

      if (!response.ok) {
        setMessage(getLoginFailureMessage(response.status));
        return;
      }

      const authResponse = await response.json();
      saveAuthSession(authResponse);
      const returnTo = getLoginDestination(authResponse.user, location.state?.returnTo);
      navigate(
        returnTo,
        {
          replace: true,
          state: returnTo?.endsWith('/book')
            ? { bookingDraft: location.state?.bookingDraft }
            : undefined,
        },
      );
    } catch {
      setMessage('Unable to connect to the server. Please try again.');
    } finally {
      submitting.current = false;
      setIsSubmitting(false);
    }
  };

  const emailError = touched.email && errors.email;
  const passwordError = touched.password && errors.password;

  return (
    <div className="auth-page container">
      <div className="auth-card">
        <p className="eyebrow">Welcome back</p>
        <h1>Login</h1>

        {location.state?.message && (
          <p className="form-message" role="status">{location.state.message}</p>
        )}

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              name="email"
              type="text"
              inputMode="email"
              value={formData.email}
              onChange={handleChange}
              onBlur={handleBlur}
              placeholder="Enter email"
              autoComplete="email"
              aria-invalid={Boolean(emailError)}
              aria-describedby={emailError ? 'email-error' : undefined}
            />
            {emailError && <span id="email-error" className="field-error">{errors.email}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <div className="password-input-wrap">
              <input
                id="password"
                name="password"
                type={showPassword ? 'text' : 'password'}
                value={formData.password}
                onChange={handleChange}
                onBlur={handleBlur}
                placeholder="Enter your password"
                autoComplete="current-password"
                aria-invalid={Boolean(passwordError)}
                aria-describedby={passwordError ? 'password-error' : undefined}
              />
              <button type="button" className="password-toggle" onClick={() => setShowPassword((shown) => !shown)}>
                {showPassword ? 'Hide' : 'Show'}
              </button>
            </div>
            {passwordError && <span id="password-error" className="field-error">{errors.password}</span>}
          </div>

          <button type="submit" className="btn btn-primary full-width" disabled={isSubmitting}>
            {isSubmitting ? 'Logging in...' : 'Login'}
          </button>

          <Link to="/register" className="btn btn-secondary full-width text-center">
            Create an account
          </Link>

          <p className="helper-text">Forgot password?</p>
        </form>

        {message && (
          <p className="form-message form-message-error" role="alert">
            {message}{' '}
            {(message.includes('register') || message.includes('Register')) && (
              <Link
                to="/register"
                state={{
                  returnTo: location.state?.returnTo,
                  bookingDraft: location.state?.bookingDraft,
                }}
              >
                Register
              </Link>
            )}
          </p>
        )}
      </div>
    </div>
  );
}

export default Login;
