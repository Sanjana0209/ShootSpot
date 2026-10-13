import { authenticatedFetch, getAuthToken, readApiError } from './auth.js';

const BOOKINGS_API_URL = 'https://shootspot.onrender.com/api/bookings';

export class AuthenticationRequiredError extends Error {
  constructor() {
    super('Your session has expired. Please log in again.');
    this.name = 'AuthenticationRequiredError';
  }
}

export class BookingConflictError extends Error {
  constructor(message) {
    super(message);
    this.name = 'BookingConflictError';
  }
}

export async function fetchMyBookings() {
  const response = await authenticatedFetch(`${BOOKINGS_API_URL}/me`);
  if (response.status === 401) {
    throw new AuthenticationRequiredError();
  }
  if (!response.ok) {
    throw new Error(await readApiError(response, 'Unable to load your bookings.'));
  }

  const bookings = await response.json();
  if (!Array.isArray(bookings)) {
    throw new Error('The bookings API returned an invalid response.');
  }
  return bookings;
}

export async function fetchBookingAvailability({
  locationId,
  bookingDate,
  startTime,
  durationHours,
  signal,
}) {
  if (!getAuthToken()) {
    throw new Error('Sign in to check live availability. Booking submission will ask you to sign in.');
  }
  const params = new URLSearchParams({
    locationId: String(locationId),
    bookingDate,
    startTime,
    durationHours: String(durationHours),
  });
  const response = await authenticatedFetch(`${BOOKINGS_API_URL}/availability?${params}`, {
    signal,
  });
  if (response.status === 401) {
    throw new AuthenticationRequiredError();
  }
  if (!response.ok) {
    throw new Error(await readApiError(response, 'Unable to check this time slot.'));
  }
  return response.json();
}

export async function submitBooking(booking) {
  if (!getAuthToken()) {
    throw new AuthenticationRequiredError();
  }
  const response = await authenticatedFetch(BOOKINGS_API_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(booking),
  });

  if (response.status === 401) {
    throw new AuthenticationRequiredError();
  }
  if (!response.ok) {
    const error = await readApiError(response, 'Unable to create your booking.');
    if (typeof error === 'object') {
      throw new Error(Object.values(error).join(' '));
    }
    if (response.status === 409) {
      throw new BookingConflictError(error);
    }
    throw new Error(error);
  }
  return response.json();
}
