const API_URL = 'http://localhost:8080/api/auth';
const TOKEN_KEY = 'shootspot.authToken';
const USER_KEY = 'shootspot.authUser';

export async function readApiError(response, fallbackMessage) {
  const errorData = await response.json().catch(() => ({}));
  if (errorData.errors && typeof errorData.errors === 'object') {
    return errorData.errors;
  }
  if (errorData.detail || errorData.message) {
    return errorData.detail || errorData.message;
  }
  if (response.status === 401) return 'Your session has expired. Please sign in again.';
  if (response.status === 403) return 'You are not authorized to make this change.';
  if (response.status === 409) return 'This change conflicts with an existing account or record.';
  if (response.status >= 500) return 'The server could not complete this request. Please try again.';
  return fallbackMessage;
}

export function saveAuthSession(authResponse) {
  if (!authResponse?.token || !authResponse?.user) {
    throw new Error('The server did not return a valid authentication session.');
  }

  sessionStorage.setItem(TOKEN_KEY, authResponse.token);
  sessionStorage.setItem(USER_KEY, JSON.stringify(authResponse.user));
}

export function getAuthToken() {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function getAuthUser() {
  try {
    const user = JSON.parse(sessionStorage.getItem(USER_KEY) || 'null');
    return user && typeof user === 'object' ? user : null;
  } catch {
    return null;
  }
}

export function clearAuthSession() {
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(USER_KEY);
}

export function logoutAndNavigate(navigate) {
  clearAuthSession();
  navigate('/login', { replace: true, state: null });
}

export async function authenticatedFetch(path, options = {}) {
  const headers = new Headers(options.headers);
  const token = getAuthToken();

  if (!token) {
    throw new Error('Your sign-in session is missing. Please sign in again.');
  }
  headers.set('Authorization', `Bearer ${token}`);

  const response = await fetch(path, { ...options, headers });
  if (response.status === 401) {
    clearAuthSession();
  }
  return response;
}

async function requireSuccessfulResponse(response, fallbackMessage) {
  if (response.ok) return response;
  const apiError = await readApiError(response, fallbackMessage);
  if (typeof apiError === 'object') {
    throw new Error(Object.values(apiError).join(' '));
  }
  throw new Error(apiError);
}

export async function fetchCurrentProfile() {
  const response = await authenticatedFetch(`${API_URL}/me`);
  await requireSuccessfulResponse(response, 'Unable to load your profile.');
  return response.json();
}

export async function updateCurrentProfile(profile) {
  const response = await authenticatedFetch(`${API_URL}/me`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(profile),
  });
  await requireSuccessfulResponse(response, 'Unable to update your profile.');
  return response.json();
}

export async function changeCurrentEmail(email, currentPassword) {
  const response = await authenticatedFetch(`${API_URL}/me/email`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, currentPassword }),
  });
  await requireSuccessfulResponse(response, 'Unable to change your email.');
  return response.json();
}

export async function changeCurrentPassword(currentPassword, newPassword, confirmNewPassword) {
  const response = await authenticatedFetch(`${API_URL}/me/password`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ currentPassword, newPassword, confirmNewPassword }),
  });
  await requireSuccessfulResponse(response, 'Unable to change your password.');
}

export async function authenticateWithCredentials(email, password) {
  const response = await fetch(`${API_URL}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: email.trim(), password }),
  });
  if (!response.ok) {
    throw new Error('Your updated credentials could not be verified. Sign in again to continue.');
  }
  return response.json();
}

export { API_URL };
