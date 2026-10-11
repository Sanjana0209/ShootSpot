import { Link, useOutletContext } from 'react-router-dom';

function Dashboard() {
  const { user } = useOutletContext();

  return (
    <div className="dashboard-page">
      <div className="dashboard-header">
        <div>
          <p className="eyebrow">Dashboard</p>
          <h1>Welcome, {user.name}!</h1>
        </div>
        <Link to="/explore" className="btn btn-primary small">
          Browse locations
        </Link>
      </div>

      <section className="panel-card account-summary">
        <h2>Your account</h2>
        <p><strong>Name:</strong> {user.name}</p>
        <p><strong>Email:</strong> {user.email}</p>
        <p className="muted-copy">
          Browse locations, review your bookings, or view your profile from the navigation.
        </p>
        <div className="quick-actions">
          <Link to="/bookings" className="btn btn-secondary small">My Bookings</Link>
          <Link to="/profile" className="btn btn-secondary small">My Profile</Link>
        </div>
      </section>
    </div>
  );
}

export default Dashboard;
