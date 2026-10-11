import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  API_URL,
  authenticatedFetch,
  getAuthToken,
  logoutAndNavigate,
  readApiError,
} from '../services/auth';

const navItems = [
  { label: 'Dashboard', to: '/admin' },
  { label: 'Locations', to: '/admin/locations' },
  { label: 'Bookings', to: '/admin/bookings' },
  { label: 'Users', to: '/admin/users' },
  { label: 'Reviews', to: '/admin/reviews' },
];

function AdminLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const [isAuthorized, setIsAuthorized] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!getAuthToken()) {
      navigate('/login', { replace: true, state: { returnTo: location.pathname } });
      return;
    }

    let isCurrent = true;
    authenticatedFetch(`${API_URL}/me`)
      .then(async (response) => {
        if (response.status === 401) {
          navigate('/login', { replace: true, state: { returnTo: location.pathname } });
          return null;
        }
        if (!response.ok) {
          throw new Error(await readApiError(response, 'Unable to verify administrator access.'));
        }
        return response.json();
      })
      .then((user) => {
        if (!isCurrent || !user) return;
        if (user.role !== 'ADMIN') {
          navigate('/dashboard', { replace: true });
          return;
        }
        setIsAuthorized(true);
      })
      .catch((requestError) => {
        if (isCurrent) setError(requestError.message || 'Unable to verify administrator access.');
      })
      .finally(() => {
        if (isCurrent) setIsLoading(false);
      });

    return () => {
      isCurrent = false;
    };
  }, [navigate, location.pathname]);

  if (!getAuthToken()) return null;
  if (isLoading) return <main className="dashboard-status" role="status">Checking administrator access...</main>;
  if (error) {
    return <main className="dashboard-status form-message-error" role="alert">{error}</main>;
  }
  if (!isAuthorized) return null;

  const handleLogout = () => {
    logoutAndNavigate(navigate);
  };

  return (
    <div className="dashboard-shell admin-shell">
      <aside className="dashboard-sidebar admin-sidebar">
        <div className="sidebar-brand">ShootSpot Admin</div>
        <nav className="sidebar-nav" aria-label="Admin navigation">
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
        <Outlet />
      </main>
    </div>
  );
}

export default AdminLayout;
