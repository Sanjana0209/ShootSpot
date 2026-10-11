import { authenticatedFetch, getAuthToken, readApiError } from './auth.js';

const FAVORITES_API_URL = 'http://localhost:8080/api/favorites';

async function requireSuccess(response, fallbackMessage) {
  if (!response.ok) {
    throw new Error(await readApiError(response, fallbackMessage));
  }
}

export async function fetchUserFavorites(signal) {
  const response = await authenticatedFetch(FAVORITES_API_URL, { signal });
  await requireSuccess(response, 'Unable to load your saved locations.');
  const favorites = await response.json();
  if (!Array.isArray(favorites)) {
    throw new Error('The favorites API returned an invalid response.');
  }
  return favorites;
}

async function changeFavorite(locationId, method) {
  if (!getAuthToken()) {
    throw new Error('Please log in to save locations to your favourites.');
  }

  const response = await authenticatedFetch(
    `${FAVORITES_API_URL}/${encodeURIComponent(locationId)}`,
    { method },
  );
  await requireSuccess(response, 'Unable to update your favourites.');
}

export function addUserFavorite(locationId) {
  return changeFavorite(locationId, 'POST');
}

export function removeUserFavorite(locationId) {
  return changeFavorite(locationId, 'DELETE');
}
