import { useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { getAuthUser, logoutAndNavigate } from '../services/auth';

const navItems = [
  { label: 'Home', to: '/' },
  { label: 'Explore', to: '/explore' },
  { label: 'How It Works', to: '/#how-it-works' },
  { label: 'About', to: '/#about' },
];

function Navbar() {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const user = getAuthUser();

  const handleLogout = () => {
    setIsMenuOpen(false);
    logoutAndNavigate(navigate);
  };

  const accountLinks = user?.role === 'ADMIN'
    ? [
      { label: 'Admin Dashboard', to: '/admin' },
      { label: 'Admin Locations', to: '/admin/locations' },
      { label: 'Admin Bookings', to: '/admin/bookings' },
      { label: 'Admin Users', to: '/admin/users' },
      { label: 'Admin Reviews', to: '/admin/reviews' },
    ]
    : user
      ? [
        { label: 'Dashboard', to: '/dashboard' },
        { label: 'My Bookings', to: '/bookings' },
        { label: 'Favorites', to: '/favorites' },
        { label: 'Profile', to: '/profile' },
      ]
      : [];

  return (
    <header className="site-header" key={location.pathname}>
      <nav className="navbar container" aria-label="Main navigation">
        <Link to="/" className="brand" aria-label="ShootSpot home">
          ShootSpot
        </Link>

        <button
          type="button"
          className="nav-toggle"
          aria-label="Toggle navigation"
          onClick={() => setIsMenuOpen((prev) => !prev)}
        >
          {isMenuOpen ? '✕' : '☰'}
        </button>

        <div className={`nav-links ${isMenuOpen ? 'open' : ''}`}>
          {[...navItems, ...accountLinks].map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              onClick={() => setIsMenuOpen(false)}
              end
            >
              {item.label}
            </NavLink>
          ))}
          {user && (
            <button type="button" className="nav-link nav-logout" onClick={handleLogout}>
              Logout
            </button>
          )}
          {!user && (
            <>
              <Link to="/login" className="nav-link nav-account-mobile" onClick={() => setIsMenuOpen(false)}>
                Login
              </Link>
              <Link to="/register" className="nav-link nav-account-mobile" onClick={() => setIsMenuOpen(false)}>
                Register
              </Link>
            </>
          )}
        </div>

        {!user && (
          <div className="nav-actions">
            <Link to="/login" className="btn btn-ghost small">
              Login
            </Link>
            <Link to="/register" className="btn btn-primary small">
              Register
            </Link>
          </div>
        )}
      </nav>
    </header>
  );
}

export default Navbar;
