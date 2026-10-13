import { useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { API_URL, readApiError } from '../services/auth';
import { getRegistrationSuccessState } from '../services/authFlow';
import { getPasswordStrength, validateRegistrationField } from '../services/validation';

const INITIAL_FORM = {
  fullName: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
};
const FIELDS = Object.keys(INITIAL_FORM);
const API_FIELD_NAMES = { name: 'fullName' };

function Register() {
  const [formData, setFormData] = useState(INITIAL_FORM);
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [message, setMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const submitting = useRef(false);
  const location = useLocation();
  const navigate = useNavigate();

  const handleChange = (event) => {
    const { name, value } = event.target;
    const nextFormData = { ...formData, [name]: value };
    setFormData(nextFormData);
    setMessage('');

    if (touched[name]) {
      setErrors((previous) => ({
        ...previous,
        [name]: validateRegistrationField(name, value, nextFormData),
      }));
    }
    if (name === 'password' && touched.confirmPassword) {
      setErrors((previous) => ({
        ...previous,
        confirmPassword: validateRegistrationField(
          'confirmPassword',
          nextFormData.confirmPassword,
          nextFormData,
        ),
      }));
    }
  };

  const handleBlur = (event) => {
    const { name, value } = event.target;
    const normalizedValue = name === 'fullName' || name === 'email' ? value.trim() : value;
    const nextFormData = { ...formData, [name]: normalizedValue };
    setFormData(nextFormData);
    setTouched((previous) => ({ ...previous, [name]: true }));
    setErrors((previous) => ({
      ...previous,
      [name]: validateRegistrationField(name, normalizedValue, nextFormData),
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (submitting.current) return;
    setMessage('');

    const normalizedForm = {
      ...formData,
      fullName: formData.fullName.trim(),
      email: formData.email.trim(),
    };
    setFormData(normalizedForm);
    setTouched(Object.fromEntries(FIELDS.map((field) => [field, true])));
    const validationErrors = Object.fromEntries(
      FIELDS.map((field) => [
        field,
        validateRegistrationField(field, normalizedForm[field], normalizedForm),
      ]),
    );
    setErrors(validationErrors);
    if (Object.values(validationErrors).some(Boolean)) return;

    submitting.current = true;
    setIsSubmitting(true);
    try {
      const response = await fetch(`${API_URL}/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: normalizedForm.fullName,
          email: normalizedForm.email,
          phone: normalizedForm.phone,
          password: normalizedForm.password,
        }),
      });

      if (!response.ok) {
        const apiError = await readApiError(
          response,
          'Registration failed. Please check your details and try again.',
        );
        if (typeof apiError === 'object') {
          const mappedErrors = {};
          Object.entries(apiError).forEach(([field, error]) => {
            mappedErrors[API_FIELD_NAMES[field] || field] = error;
          });
          setErrors((previous) => ({ ...previous, ...mappedErrors }));
          setMessage('Please correct the highlighted fields.');
        } else {
          setMessage(apiError);
        }
        return;
      }

      navigate('/login', {
        replace: true,
        state: getRegistrationSuccessState(
          normalizedForm.email,
          location.state?.returnTo,
          location.state?.bookingDraft,
        ),
      });
    } catch {
      setMessage('Unable to connect to the server. Please try again.');
    } finally {
      submitting.current = false;
      setIsSubmitting(false);
    }
  };

  const fieldError = (field) => touched[field] && errors[field];
  const passwordStrength = getPasswordStrength(formData.password);

  return (
    <div className="auth-page container">
      <div className="auth-card">
        <p className="eyebrow">New account</p>
        <h1>Register</h1>

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label htmlFor="fullName">Full name</label>
            <input
              id="fullName"
              name="fullName"
              type="text"
              value={formData.fullName}
              onChange={handleChange}
              onBlur={handleBlur}
              placeholder="Your full name"
              autoComplete="name"
              aria-invalid={Boolean(fieldError('fullName'))}
              aria-describedby={fieldError('fullName') ? 'fullName-error' : undefined}
            />
            {fieldError('fullName') && <span id="fullName-error" className="field-error">{errors.fullName}</span>}
          </div>

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
              placeholder="you@example.com"
              autoComplete="email"
              aria-invalid={Boolean(fieldError('email'))}
              aria-describedby={fieldError('email') ? 'email-error' : 'email-help'}
            />
            {fieldError('email') && <span id="email-error" className="field-error">{errors.email}</span>}
            <span id="email-help" className="helper-text form-helper">
              Format checks cannot confirm that this inbox exists.
            </span>
          </div>

          <div className="form-group">
            <label htmlFor="phone">Mobile number</label>
            <input
              id="phone"
              name="phone"
              type="tel"
              value={formData.phone}
              onChange={handleChange}
              onBlur={handleBlur}
              placeholder="10-digit Indian mobile number"
              autoComplete="tel-national"
              aria-invalid={Boolean(fieldError('phone'))}
              aria-describedby={fieldError('phone') ? 'phone-error' : undefined}
            />
            {fieldError('phone') && <span id="phone-error" className="field-error">{errors.phone}</span>}
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
                placeholder="Create a password"
                autoComplete="new-password"
                aria-invalid={Boolean(fieldError('password'))}
                aria-describedby={fieldError('password') ? 'password-error' : undefined}
              />
              <button type="button" className="password-toggle" onClick={() => setShowPassword((shown) => !shown)}>
                {showPassword ? 'Hide' : 'Show'}
              </button>
            </div>
            {fieldError('password') && <span id="password-error" className="field-error">{errors.password}</span>}
            {passwordStrength && (
              <span className={`password-strength strength-${passwordStrength.toLowerCase()}`}>
                Password strength: {passwordStrength}
              </span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword">Confirm password</label>
            <div className="password-input-wrap">
              <input
                id="confirmPassword"
                name="confirmPassword"
                type={showConfirmPassword ? 'text' : 'password'}
                value={formData.confirmPassword}
                onChange={handleChange}
                onBlur={handleBlur}
                placeholder="Confirm your password"
                autoComplete="new-password"
                aria-invalid={Boolean(fieldError('confirmPassword'))}
                aria-describedby={fieldError('confirmPassword') ? 'confirmPassword-error' : undefined}
              />
              <button type="button" className="password-toggle" onClick={() => setShowConfirmPassword((shown) => !shown)}>
                {showConfirmPassword ? 'Hide' : 'Show'}
              </button>
            </div>
            {fieldError('confirmPassword') && (
              <span id="confirmPassword-error" className="field-error">{errors.confirmPassword}</span>
            )}
          </div>

          <button type="submit" className="btn btn-primary full-width" disabled={isSubmitting}>
            {isSubmitting ? 'Creating account...' : 'Create Account'}
          </button>
        </form>

        {message && <p className="form-message form-message-error" role="alert">{message}</p>}
        <Link
          to="/login"
          state={{
            returnTo: location.state?.returnTo,
            bookingDraft: location.state?.bookingDraft,
          }}
          className="btn btn-secondary full-width text-center"
        >
          Already have an account? Login
        </Link>
      </div>
    </div>
  );
}

export default Register;
