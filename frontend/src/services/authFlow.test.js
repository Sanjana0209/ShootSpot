import assert from 'node:assert/strict';
import test from 'node:test';
import {
  getLoginDestination,
  getLoginFailureMessage,
  getLoginInitialForm,
  getRegistrationSuccessState,
} from './authFlow.js';

test('login fields are empty on a fresh visit and never prefill a password', () => {
  assert.deepEqual(getLoginInitialForm(null), { email: '', password: '' });
  assert.deepEqual(
    getLoginInitialForm({ email: 'registered@example.com', password: 'must-not-use' }),
    { email: 'registered@example.com', password: '' },
  );
});

test('logout navigation clears route email state and opens Login with empty fields', async () => {
  const { logoutAndNavigate } = await import('./auth.js');
  const session = new Map([
    ['shootspot.authToken', 'jwt-token'],
    ['shootspot.authUser', JSON.stringify({ email: 'registered@example.com' })],
  ]);
  const originalSessionStorage = globalThis.sessionStorage;
  globalThis.sessionStorage = {
    getItem: (key) => session.get(key) ?? null,
    removeItem: (key) => session.delete(key),
  };

  let navigation;
  try {
    logoutAndNavigate((path, options) => {
      navigation = { path, options };
    });
  } finally {
    globalThis.sessionStorage = originalSessionStorage;
  }

  assert.equal(session.size, 0);
  assert.deepEqual(navigation, {
    path: '/login',
    options: { replace: true, state: null },
  });
  assert.deepEqual(getLoginInitialForm(navigation.options.state), { email: '', password: '' });
});

test('registration success sends confirmation and preserves safe login context', () => {
  assert.deepEqual(getRegistrationSuccessState(
    'new-user@example.com',
    '/locations/42/book',
    { date: '2030-01-01' },
  ), {
    message: 'Registration successful. Please log in.',
    email: 'new-user@example.com',
    returnTo: '/locations/42/book',
    bookingDraft: { date: '2030-01-01' },
  });
});

test('login failure stays generic and does not disclose whether the account exists', () => {
  assert.equal(getLoginFailureMessage(401), getLoginFailureMessage(403));
  assert.match(getLoginFailureMessage(401), /register/i);
});

test('existing users and admins return to their role landing routes', () => {
  assert.equal(getLoginDestination({ role: 'USER' }), '/dashboard');
  assert.equal(getLoginDestination({ role: 'ADMIN' }), '/admin');
});

test('login preserves a safe protected return route without allowing auth redirects', () => {
  assert.equal(
    getLoginDestination({ role: 'USER' }, '/locations/42/book'),
    '/locations/42/book',
  );
  assert.equal(
    getLoginDestination({ role: 'ADMIN' }, '/admin/bookings'),
    '/admin/bookings',
  );
  assert.equal(getLoginDestination({ role: 'USER' }, '/login'), '/dashboard');
  assert.equal(getLoginDestination({ role: 'USER' }, '//attacker.example'), '/dashboard');
});
