import { useEffect, useState } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import {
  authenticateWithCredentials,
  changeCurrentEmail,
  changeCurrentPassword,
  clearAuthSession,
  fetchCurrentProfile,
  saveAuthSession,
  updateCurrentProfile,
} from '../services/auth';

const NAME_PATTERN = /^[\p{L} ]+$/u;
const PHONE_PATTERN = /^[6-9][0-9]{9}$/;
const EMAIL_PATTERN = /^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$/;
const PASSWORD_PATTERN = /^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$/;

function getProfileErrors(name, phone) {
  const errors = {};
  const normalizedName = name.trim();
  if (!normalizedName) errors.name = 'Full name is required.';
  else if (normalizedName.length < 3 || normalizedName.length > 30) {
    errors.name = 'Name must be between 3 and 30 characters.';
  } else if (!NAME_PATTERN.test(normalizedName)) {
    errors.name = 'Name may contain letters and spaces only.';
  }

  if (!phone) errors.phone = 'Mobile number is required.';
  else if (!PHONE_PATTERN.test(phone)) {
    errors.phone = 'Enter a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9.';
  }
  return errors;
}

function getPasswordErrors(password, confirmation) {
  const errors = {};
  if (!password) errors.newPassword = 'New password is required.';
  else if (password.length < 8 || password.length > 16) {
    errors.newPassword = 'Password must be between 8 and 16 characters.';
  } else if (!PASSWORD_PATTERN.test(password)) {
    errors.newPassword = 'Include uppercase, lowercase, a number, and a special character.';
  }
  if (!confirmation) errors.confirmNewPassword = 'Please confirm your new password.';
  else if (password !== confirmation) errors.confirmNewPassword = 'Passwords do not match.';
  return errors;
}

function PasswordField({
  id,
  label,
  value,
  onChange,
  visible,
  onToggleVisibility,
  disabled,
  autoComplete,
  error,
  required = false,
}) {
  return (
    <div className="form-group">
      <label htmlFor={id}>{label}</label>
      <div className="password-input-wrap">
        <input
          id={id}
          type={visible ? 'text' : 'password'}
          value={value}
          onChange={onChange}
          autoComplete={autoComplete}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${id}-error` : undefined}
          disabled={disabled}
          required={required}
        />
        <button
          type="button"
          className="password-toggle"
          onClick={onToggleVisibility}
          disabled={disabled}
          aria-pressed={visible}
        >
          {visible ? 'Hide' : 'Show'} {label.toLowerCase()}
        </button>
      </div>
      {error && <small id={`${id}-error`} className="field-error">{error}</small>}
    </div>
  );
}

function Profile() {
  const { user, updateUser } = useOutletContext();
  const navigate = useNavigate();
  const [name, setName] = useState(user.name || '');
  const [phone, setPhone] = useState(user.phone || '');
  const [email, setEmail] = useState(user.email || '');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmNewPassword, setConfirmNewPassword] = useState('');
  const [visiblePasswords, setVisiblePasswords] = useState({});
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    let isCurrent = true;
    fetchCurrentProfile()
      .then((latestUser) => {
        if (!isCurrent) return;
        updateUser(latestUser);
        setName(latestUser.name || '');
        setPhone(latestUser.phone || '');
        setEmail(latestUser.email || '');
      })
      .catch((requestError) => {
        if (isCurrent) setError(requestError.message || 'Unable to load your profile.');
      })
      .finally(() => {
        if (isCurrent) setIsLoading(false);
      });
    return () => {
      isCurrent = false;
    };
  }, [updateUser]);

  const togglePasswordVisibility = (field) => {
    setVisiblePasswords((previous) => ({ ...previous, [field]: !previous[field] }));
  };

  const handleSaveChanges = async (event) => {
    event.preventDefault();
    setError('');
    setMessage('');
    const normalizedEmail = email.trim();
    const emailChanged = normalizedEmail.toLowerCase() !== String(user.email || '').trim().toLowerCase();
    const passwordChanged = newPassword.length > 0;
    const nameChanged = name.trim() !== String(user.name || '').trim();
    const phoneChanged = phone !== String(user.phone || '');
    const profileChanged = nameChanged || phoneChanged;
    const errors = profileChanged ? getProfileErrors(name, phone) : {};

    if (emailChanged) {
      if (!normalizedEmail) errors.email = 'Email is required.';
      else if (normalizedEmail.length > 254 || !EMAIL_PATTERN.test(normalizedEmail)) {
        errors.email = 'Enter a valid email address (maximum 254 characters).';
      }
    }
    if ((emailChanged || passwordChanged) && !currentPassword) {
      errors.currentPassword = 'Current password is required for this change.';
    }
    if (passwordChanged) {
      Object.assign(errors, getPasswordErrors(newPassword, confirmNewPassword));
    } else if (confirmNewPassword) {
      errors.newPassword = 'Enter a new password to change your password.';
    }
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setIsSaving(true);
    const completedOperations = [];
    let currentOperation = 'profile details';
    try {
      if (profileChanged) {
        currentOperation = 'profile details (PUT /api/auth/me)';
        const updatedUser = await updateCurrentProfile({ name: name.trim(), phone });
        updateUser(updatedUser);
        setName(updatedUser.name);
        setPhone(updatedUser.phone || '');
        completedOperations.push('profile details');
      }

      if (emailChanged) {
        currentOperation = 'email change (PUT /api/auth/me/email)';
        const updatedEmailUser = await changeCurrentEmail(normalizedEmail, currentPassword);
        updateUser(updatedEmailUser);
        setEmail(updatedEmailUser.email);
        completedOperations.push('email');

        if (passwordChanged) {
          currentOperation = 'session refresh after email change (POST /api/auth/login)';
          const authResponse = await authenticateWithCredentials(normalizedEmail, currentPassword);
          saveAuthSession(authResponse);
          updateUser(authResponse.user);
        }
      }

      if (passwordChanged) {
        currentOperation = 'password change (PUT /api/auth/me/password)';
        await changeCurrentPassword(currentPassword, newPassword, confirmNewPassword);
        completedOperations.push('password');
      }

      if (emailChanged || passwordChanged) {
        clearAuthSession();
        const changes = completedOperations.length
          ? `${completedOperations.join(', ')} ${completedOperations.length === 1 ? 'was' : 'were'} saved.`
          : 'Your changes were saved.';
        const credentialsChanged = [
          emailChanged && 'email',
          passwordChanged && 'password',
        ].filter(Boolean).join(' and ');
        navigate('/login', {
          replace: true,
          state: {
            message: `${changes} Your ${credentialsChanged} ${credentialsChanged.includes(' and ') ? 'have' : 'has'} changed. Sign in again to continue.`,
          },
        });
        return;
      }

      if (completedOperations.length) {
        setMessage('Your profile details were saved.');
      } else {
        setMessage('There are no changes to save.');
      }
    } catch (requestError) {
      const completedVerb = completedOperations.length === 1
        && completedOperations[0] !== 'profile details' ? 'was' : 'were';
      const completedMessage = completedOperations.length
        ? `${completedOperations.join(', ')} ${completedVerb} saved, but ${currentOperation} failed: `
        : `${currentOperation} failed: `;
      setError(`${completedMessage}${requestError.message || 'Unable to save your changes.'}`);
      if (emailChanged && completedOperations.includes('email')) {
        clearAuthSession();
        navigate('/login', {
          replace: true,
          state: {
            message: `Your ${completedOperations.join(' and ')} ${completedOperations.length === 1 ? 'was' : 'were'} saved. The password change could not be completed. Sign in with your new email and current password, then try changing your password again.`,
          },
        });
        return;
      }
      if (passwordChanged && completedOperations.includes('password')) {
        clearAuthSession();
        navigate('/login', {
          replace: true,
          state: {
            message: `Your ${completedOperations.join(' and ')} were saved. Sign in again with your new credentials.`,
          },
        });
        return;
      }
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return <main className="dashboard-status" role="status">Loading your profile...</main>;
  }

  return (
    <div className="container section-block profile-page">
      <div className="section-heading">
        <h1>My Profile</h1>
      </div>

      {message && <p className="form-message" role="status">{message}</p>}
      {error && <p className="form-message form-message-error" role="alert">{error}</p>}

      <section className="profile-card profile-settings-card">
        <form className="profile-form" onSubmit={handleSaveChanges} noValidate>
          <div className="profile-details">
            <div className="form-group">
              <label htmlFor="profile-name">Full Name</label>
              <input
                id="profile-name"
                type="text"
                value={name}
                minLength={3}
                maxLength={30}
                autoComplete="name"
                aria-invalid={Boolean(fieldErrors.name)}
                onChange={(event) => setName(event.target.value)}
                onBlur={() => setFieldErrors((current) => ({
                  ...current,
                  name: getProfileErrors(name, phone).name || '',
                }))}
                disabled={isSaving}
                required
              />
              {fieldErrors.name && <small className="field-error">{fieldErrors.name}</small>}
            </div>
            <div className="form-group">
              <label htmlFor="profile-email">Email Address</label>
              <input
                id="profile-email"
                type="email"
                value={email}
                maxLength={254}
                autoComplete="email"
                aria-invalid={Boolean(fieldErrors.email)}
                onChange={(event) => {
                  setEmail(event.target.value);
                  setFieldErrors((current) => ({ ...current, email: '' }));
                }}
                disabled={isSaving}
                required
              />
              {fieldErrors.email && <small className="field-error">{fieldErrors.email}</small>}
            </div>
            <div className="form-group">
              <label htmlFor="profile-phone">Mobile Number</label>
              <input
                id="profile-phone"
                type="tel"
                inputMode="numeric"
                value={phone}
                maxLength={10}
                autoComplete="tel-national"
                aria-invalid={Boolean(fieldErrors.phone)}
                onChange={(event) => setPhone(event.target.value)}
                onBlur={() => setFieldErrors((current) => ({
                  ...current,
                  phone: getProfileErrors(name, phone).phone || '',
                }))}
                disabled={isSaving}
                required
              />
              {fieldErrors.phone && <small className="field-error">{fieldErrors.phone}</small>}
            </div>
            <PasswordField
              id="profile-current-password"
              label="Current Password"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
              visible={Boolean(visiblePasswords.currentPassword)}
              onToggleVisibility={() => togglePasswordVisibility('currentPassword')}
              disabled={isSaving}
              autoComplete="current-password"
              error={fieldErrors.currentPassword}
            />
            <PasswordField
              id="profile-new-password"
              label="New Password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              visible={Boolean(visiblePasswords.newPassword)}
              onToggleVisibility={() => togglePasswordVisibility('newPassword')}
              disabled={isSaving}
              autoComplete="new-password"
              error={fieldErrors.newPassword}
            />
            <PasswordField
              id="profile-confirm-password"
              label="Confirm New Password"
              value={confirmNewPassword}
              onChange={(event) => setConfirmNewPassword(event.target.value)}
              visible={Boolean(visiblePasswords.confirmNewPassword)}
              onToggleVisibility={() => togglePasswordVisibility('confirmNewPassword')}
              disabled={isSaving}
              autoComplete="new-password"
              error={fieldErrors.confirmNewPassword}
            />
            <button type="submit" className="btn btn-primary profile-action" disabled={isSaving}>
              {isSaving ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

export default Profile;
