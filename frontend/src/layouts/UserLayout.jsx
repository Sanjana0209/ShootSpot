import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  API_URL,
  authenticatedFetch,
  getAuthToken,
  logoutAndNavigate,
  readApiError,
} from '../services/auth';

const navItems = [
  { label: 'Dashboard', to: '/dashboard' },
  { label: 'Browse Locations', to: '/explore' },
  { label: 'My Bookings', to: '/bookings' },
  { label: 'Favorites', to: '/favorites' },
  { label: 'My Profile', to: '/profile' },
];

function UserLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const locationRef = useRef(location);
  const [user, setUser] = useState(null);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    locationRef.current = location;
  }, [location]);

  useEffect(() => {
    const getLoginState = () => ({
      returnTo: locationRef.current.pathname,
      bookingDraft: locationRef.current.state?.bookingDraft,
    });

    if (!getAuthToken()) {
      navigate('/login', { replace: true, state: getLoginState() });
      return;
    }

    let isCurrent = true;
    authenticatedFetch(`${API_URL}/me`)
      .then(async (response) => {
        if (!response.ok) {
          if (response.status === 401) {
            navigate('/login', { replace: true, state: getLoginState() });
            return null;
          }
          throw new Error(await readApiError(response, 'Unable to load your account.'));
        }
        return response.json();
      })
      .then((authenticatedUser) => {
        if (isCurrent && authenticatedUser) setUser(authenticatedUser);
      })
      .catch((requestError) => {
        if (isCurrent) setError(requestError.message || 'Unable to load your account.');
      })
      .finally(() => {
        if (isCurrent) setIsLoading(false);
      });

    return () => {
      isCurrent = false;
    };
  }, [navigate]);

  const handleLogout = () => {
    logoutAndNavigate(navigate);
  };

  if (!getAuthToken()) return null;
  if (isLoading) {
    return <main className="dashboard-status" role="status">Loading your dashboard...</main>;
  }
  if (error) {
    return <main className="dashboard-status form-message-error" role="alert">{error}</main>;
  }
  if (!user) return null;

  return (
    <div className="dashboard-shell">
      <aside className="dashboard-sidebar">
        <div className="sidebar-brand">ShootSpot</div>
        <nav className="sidebar-nav" aria-label="User navigation">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
            >
              {item.label}
            </NavLink>
          ))}
          <button type="button" className="sidebar-link sidebar-logout" onClick={handleLogout}>
            Logout
          </button>
        </nav>
      </aside>

      <main className="dashboard-content">
        <Outlet context={{ user, updateUser: setUser }} />
      </main>
    </div>
  );
}

export default UserLayout;
