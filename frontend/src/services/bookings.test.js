import assert from 'node:assert/strict';
import { afterEach, test } from 'node:test';
import {
  AuthenticationRequiredError,
  BookingConflictError,
  fetchBookingAvailability,
  submitBooking,
} from './bookings.js';

const originalSessionStorage = globalThis.sessionStorage;
const originalFetch = globalThis.fetch;

function useSessionToken(token) {
  const values = new Map(token ? [['shootspot.authToken', token]] : []);
  globalThis.sessionStorage = {
    getItem: (key) => values.get(key) ?? null,
    removeItem: (key) => values.delete(key),
    setItem: (key, value) => values.set(key, value),
  };
}

afterEach(() => {
  globalThis.sessionStorage = originalSessionStorage;
  globalThis.fetch = originalFetch;
});

test('submits backend-compatible 24-hour time and returns an HTTP 409 as a booking conflict', async () => {
  useSessionToken('test-token');
  let submittedRequest;
  globalThis.fetch = async (_url, options) => {
    submittedRequest = options;
    return new Response(
      JSON.stringify({ detail: 'Already booked for this time slot. Please choose another time.' }),
      { status: 409, headers: { 'Content-Type': 'application/json' } },
    );
  };

  await assert.rejects(
    submitBooking({
      locationId: 15,
      bookingDate: '2026-10-13',
      startTime: '13:30',
      durationHours: 2,
    }),
    (error) => {
      assert.equal(error.message, 'Already booked for this time slot. Please choose another time.');
      assert.equal(error instanceof BookingConflictError, true);
      assert.equal(error instanceof AuthenticationRequiredError, false);
      return true;
    },
  );

  assert.equal(submittedRequest.method, 'POST');
  assert.deepEqual(JSON.parse(submittedRequest.body), {
    locationId: 15,
    bookingDate: '2026-10-13',
    startTime: '13:30',
    durationHours: 2,
  });
});

test('missing credentials request login only when submitting a booking', async () => {
  useSessionToken(null);
  globalThis.fetch = async () => {
    throw new Error('Unauthenticated submission must not issue a fetch');
  };

  await assert.rejects(
    submitBooking({
      locationId: 15,
      bookingDate: '2026-10-13',
      startTime: '00:00',
      durationHours: 1,
    }),
    AuthenticationRequiredError,
  );
  await assert.rejects(
    fetchBookingAvailability({
      locationId: 15,
      bookingDate: '2026-10-13',
      startTime: '10:00',
      durationHours: 2,
    }),
    (error) => {
      assert.match(error.message, /Sign in to check live availability/);
      assert.equal(error instanceof AuthenticationRequiredError, false);
      return true;
    },
  );
});

test('a genuine backend 401 is classified as an authentication requirement', async () => {
  useSessionToken('expired-token');
  globalThis.fetch = async () => new Response('', { status: 401 });

  await assert.rejects(
    submitBooking({
      locationId: 15,
      bookingDate: '2026-10-13',
      startTime: '12:00',
      durationHours: 1,
    }),
    AuthenticationRequiredError,
  );
  assert.equal(globalThis.sessionStorage.getItem('shootspot.authToken'), null);
});
