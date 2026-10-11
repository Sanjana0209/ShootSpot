const LOCATIONS_API_URL = 'http://https://shootspot.onrender.com/api/locations';

export class LocationNotFoundError extends Error {
  constructor() {
    super('This location was not found. It may have been removed or the link may be incorrect.');
    this.name = 'LocationNotFoundError';
  }
}

async function readErrorMessage(response, fallback) {
  const body = await response.json().catch(() => ({}));
  return body.detail || body.message || `${fallback} (HTTP ${response.status}).`;
}

export async function fetchLocations(signal) {
  const response = await fetch(LOCATIONS_API_URL, { signal });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Unable to load locations'));
  }

  const locations = await response.json();
  if (!Array.isArray(locations)) {
    throw new Error('The locations API returned an invalid response.');
  }
  return locations;
}

export async function fetchLocationById(id, signal) {
  const locationId = String(id || '');
  if (!/^\d+$/.test(locationId) || /^0+$/.test(locationId)) {
    throw new LocationNotFoundError();
  }

  const response = await fetch(`${LOCATIONS_API_URL}/${encodeURIComponent(locationId)}`, { signal });
  if (response.status === 404) {
    throw new LocationNotFoundError();
  }
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Unable to load this location'));
  }

  const location = await response.json();
  if (!location || typeof location !== 'object' || location.id == null) {
    throw new Error('The location API returned an invalid response.');
  }
  return location;
}
