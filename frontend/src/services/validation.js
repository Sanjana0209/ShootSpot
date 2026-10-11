const EMAIL_PATTERN =
  /^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$/;
const NAME_PATTERN = /^[\p{L} ]+$/u;
const PHONE_PATTERN = /^[6-9][0-9]{9}$/;

export function validateEmail(value) {
  const email = value.trim();
  if (!email) return 'Email is required.';
  if (email.length > 254) return 'Email must be 254 characters or fewer.';
  if (/\s/.test(email) || !EMAIL_PATTERN.test(email)) {
    return 'Enter a valid email address, such as sai@gmail.com.';
  }
  return '';
}

export function validateRegistrationField(field, value, formData) {
  switch (field) {
    case 'fullName': {
      const name = value.trim();
      if (!name) return 'Full name is required.';
      if (name.length < 3) return 'Full name must be at least 3 characters.';
      if (name.length > 30) return 'Full name must be 30 characters or fewer.';
      if (!NAME_PATTERN.test(name)) return 'Use letters and spaces only.';
      return '';
    }
    case 'email':
      return validateEmail(value);
    case 'phone':
      if (!value) return 'Mobile number is required.';
      if (!PHONE_PATTERN.test(value)) {
        return 'Enter exactly 10 digits starting with 6, 7, 8, or 9.';
      }
      return '';
    case 'password':
      if (!value) return 'Password is required.';
      if (value.length < 8) return 'Password must be at least 8 characters.';
      if (value.length > 16) return 'Password must be 16 characters or fewer.';
      if (!/[A-Z]/.test(value)) return 'Add at least one uppercase letter.';
      if (!/[a-z]/.test(value)) return 'Add at least one lowercase letter.';
      if (!/[0-9]/.test(value)) return 'Add at least one number.';
      if (!/[^A-Za-z0-9]/.test(value)) return 'Add at least one special character.';
      return '';
    case 'confirmPassword':
      if (!value) return 'Please confirm your password.';
      if (value !== formData.password) return 'Passwords do not match.';
      return '';
    default:
      return '';
  }
}

export function getPasswordStrength(password) {
  if (!password) return '';
  const criteria = [
    password.length >= 8 && password.length <= 16,
    /[A-Z]/.test(password),
    /[a-z]/.test(password),
    /[0-9]/.test(password),
    /[^A-Za-z0-9]/.test(password),
  ].filter(Boolean).length;

  if (criteria <= 2) return 'Weak';
  if (criteria < 5) return 'Medium';
  return 'Strong';
}
