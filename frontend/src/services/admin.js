import { authenticatedFetch } from './auth';

const ADMIN_API_URL = 'http://https://shootspot.onrender.com/api/admin';
const LOCATIONS_API_URL = 'http://https://shootspot.onrender.com/api/locations';

async function parseResponse(response, fallback) {
  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('Your session has expired. Please sign in again.');
    }
    if (response.status === 403) {
      throw new Error(
        'The backend denied this admin request. It checks the account role in the database configured for the running API; verify the API is connected to the expected database and that this account role is ADMIN.',
      );
    }
    if (response.status >= 500) {
      throw new Error('The server could not load admin data. Check that the backend and database are available.');
    }
    const errorData = await response.json().catch(() => ({}));
    const validationErrors = errorData.errors && typeof errorData.errors === 'object'
      ? Object.values(errorData.errors).join(' ')
      : '';
    const message = validationErrors || errorData.detail || errorData.message || fallback;
    throw new Error(message);
  }
  if (response.status === 204) return null;
  return response.json();
}

export async function fetchAdminDashboard() {
  try {
    const response = await authenticatedFetch(`${ADMIN_API_URL}/dashboard`);
    const summary = await parseResponse(response, 'Unable to load the admin dashboard.');
    const fields = [
      'totalLocations',
      'totalBookings',
      'pendingBookings',
      'confirmedBookings',
    ];
    if (!summary || fields.some((field) => !Number.isFinite(summary[field]))) {
      throw new Error('The server returned dashboard data in an unexpected format.');
    }
    return summary;
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error('Could not reach the backend. Check that the ShootSpot API is running.');
    }
    throw error;
  }
}

export async function fetchAdminBookings() {
  const response = await authenticatedFetch(`${ADMIN_API_URL}/bookings`);
  return parseResponse(response, 'Unable to load bookings.');
}

export async function updateAdminBookingStatus(id, status) {
  const response = await authenticatedFetch(`${ADMIN_API_URL}/bookings/${id}/status`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  });
  return parseResponse(response, 'Unable to update booking status.');
}

export async function saveLocation(location, id) {
  const response = await authenticatedFetch(
    id ? `${LOCATIONS_API_URL}/${id}` : LOCATIONS_API_URL,
    {
      method: id ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(location),
    },
  );
  return parseResponse(response, 'Unable to save location.');
}

export async function deleteLocation(id) {
  const response = await authenticatedFetch(`${LOCATIONS_API_URL}/${id}`, {
    method: 'DELETE',
  });
  return parseResponse(response, 'Unable to delete location.');
}
