import { authenticatedFetch } from './auth';
import { AuthenticationRequiredError } from './bookings';

const API_ROOT = 'http://https://shootspot.onrender.com/api';

async function responseError(response, fallback) {
  const errorData = await response.json().catch(() => ({}));
  const validationErrors = errorData.errors && typeof errorData.errors === 'object'
    ? Object.values(errorData.errors).join(' ')
    : '';
  return validationErrors || errorData.detail || errorData.message || fallback;
}

export async function fetchLocationReviews(locationId, signal) {
  const response = await fetch(`${API_ROOT}/locations/${encodeURIComponent(locationId)}/reviews`, { signal });
  if (!response.ok) {
    throw new Error(await responseError(response, 'Unable to load reviews.'));
  }
  const reviews = await response.json();
  if (!Array.isArray(reviews)) {
    throw new Error('The reviews API returned an invalid response.');
  }
  return reviews;
}

export async function submitLocationReview(locationId, review) {
  const response = await authenticatedFetch(
    `${API_ROOT}/locations/${encodeURIComponent(locationId)}/reviews`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(review),
    },
  );
  if (response.status === 401) {
    throw new AuthenticationRequiredError();
  }
  if (!response.ok) {
    throw new Error(await responseError(response, 'Unable to submit your review.'));
  }
  return response.json();
}

export async function submitBookingReview(bookingId, review) {
  const response = await authenticatedFetch(`${API_ROOT}/bookings/${encodeURIComponent(bookingId)}/review`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(review),
  });
  if (response.status === 401) {
    throw new AuthenticationRequiredError();
  }
  if (!response.ok) {
    throw new Error(await responseError(response, 'Unable to submit your review.'));
  }
  return response.json();
}

export async function fetchAdminReviews() {
  const response = await authenticatedFetch(`${API_ROOT}/admin/reviews`);
  if (!response.ok) {
    throw new Error(await responseError(response, 'Unable to load reviews.'));
  }
  const reviews = await response.json();
  if (!Array.isArray(reviews)) {
    throw new Error('The reviews API returned an invalid response.');
  }
  return reviews;
}

export async function removeAdminReview(reviewId) {
  const response = await authenticatedFetch(`${API_ROOT}/admin/reviews/${encodeURIComponent(reviewId)}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw new Error(await responseError(response, 'Unable to remove the review.'));
  }
}
