import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchAdminDashboard } from '../services/admin';

function AdminDashboard() {
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  const loadDashboard = useCallback(async () => {
    setError('');
    try {
      setSummary(await fetchAdminDashboard());
    } catch (requestError) {
      setError(requestError.message || 'Unable to load dashboard totals.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let isCurrent = true;
    fetchAdminDashboard()
      .then((data) => {
        if (isCurrent) setSummary(data);
      })
      .catch((requestError) => {
        if (isCurrent) setError(requestError.message || 'Unable to load dashboard totals.');
      })
      .finally(() => {
        if (isCurrent) setIsLoading(false);
      });
    return () => {
      isCurrent = false;
    };
  }, []);

  return (
    <div className="dashboard-page admin-page">
      <div className="dashboard-header">
        <div>
          <p className="eyebrow">Admin dashboard</p>
          <h1>Overview</h1>
        </div>
        <div className="quick-actions">
          <Link to="/admin/locations" className="btn btn-secondary small">Manage locations</Link>
          <Link to="/admin/bookings" className="btn btn-primary small">Manage bookings</Link>
          <button
            type="button"
            className="btn btn-secondary small"
            disabled={isLoading}
            onClick={() => {
              setIsLoading(true);
              loadDashboard();
            }}
          >
            {isLoading ? 'Loading...' : 'Refresh totals'}
          </button>
        </div>
      </div>

      {error && <p className="form-message form-message-error" role="alert">{error}</p>}

      {summary ? (
        <section className="summary-grid admin-summary-grid">
          <div className="summary-card">
            <span>Total Locations</span>
            <strong>{summary.totalLocations}</strong>
          </div>
          <div className="summary-card">
            <span>Total Bookings</span>
            <strong>{summary.totalBookings}</strong>
          </div>
          <div className="summary-card">
            <span>Pending Bookings</span>
            <strong>{summary.pendingBookings}</strong>
          </div>
          <div className="summary-card">
            <span>Confirmed Bookings</span>
            <strong>{summary.confirmedBookings}</strong>
          </div>
        </section>
      ) : isLoading ? (
        <div className="empty-state" role="status"><h3>Loading dashboard totals...</h3></div>
      ) : (
        <div className="empty-state" role="alert">
          <h3>Dashboard totals are unavailable</h3>
          <p>Use Refresh totals to try again after checking the displayed error.</p>
        </div>
      )}

      <section className="panel-card admin-dashboard-links">
        <h2>Administration</h2>
        <p>Location and booking totals are loaded from the ShootSpot database.</p>
        <div className="quick-actions">
          <Link to="/admin/locations" className="btn btn-secondary">Add or edit locations</Link>
          <Link to="/admin/bookings" className="btn btn-secondary">Review customer bookings</Link>
        </div>
      </section>
    </div>
  );
}

export default AdminDashboard;
