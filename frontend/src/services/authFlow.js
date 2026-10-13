export function getLoginDestination(user, requestedRoute) {
  const safeRoutes = [
    /^\/dashboard$/,
    /^\/explore$/,
    /^\/bookings$/,
    /^\/favorites$/,
    /^\/profile$/,
    /^\/locations\/\d+(?:\/book)?$/,
    /^\/admin(?:\/locations|\/bookings|\/users|\/reviews)?$/,
  ];
  const returnTo = typeof requestedRoute === 'string'
    && safeRoutes.some((pattern) => pattern.test(requestedRoute))
    ? requestedRoute
    : null;

  return returnTo || (user?.role === 'ADMIN' ? '/admin' : '/dashboard');
}

export function getLoginFailureMessage(status) {
  if (status === 401 || status === 403) {
    return "Invalid email or password. If you don't have an account, register.";
  }
  return 'Login is temporarily unavailable. Please try again.';
}

export function getRegistrationSuccessState(email, returnTo, bookingDraft) {
  return {
    message: 'Registration successful. Please log in.',
    email,
    returnTo,
    bookingDraft,
  };
}


export function getLoginInitialForm() {
  return {
    email: '',
    password: '',
  };
}
