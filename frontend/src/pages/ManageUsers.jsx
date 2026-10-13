
import { useEffect, useState } from 'react';
import { fetchAdminUsers } from '../services/admin';

function ManageUsers() {
  const [users, setUsers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let isCurrent = true;

    async function loadUsers() {
      try {
        const loadedUsers = await fetchAdminUsers();

        if (isCurrent) {
          setUsers(loadedUsers);
          setError('');
        }
      } catch (requestError) {
        if (isCurrent) {
          setError(requestError.message || 'Unable to load users.');
        }
      } finally {
        if (isCurrent) {
          setIsLoading(false);
        }
      }
    }

    loadUsers();

    return () => {
      isCurrent = false;
    };
  }, []);

  return (
    <div className="admin-manage-page">
      <div className="dashboard-header">
        <div>
          <p className="eyebrow">Manage users</p>
          <h1>Users</h1>
        </div>
      </div>

      {error && (
        <p className="form-message form-message-error" role="alert">
          {error}
        </p>
      )}

      <section className="panel-card table-panel">
        {isLoading ? (
          <p role="status">Loading users...</p>
        ) : error ? (
          <p>Unable to display users. Please try again later.</p>
        ) : users.length === 0 ? (
          <p>No users have registered yet.</p>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>User ID</th>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Role</th>
                  <th>Status</th>
                </tr>
              </thead>

              <tbody>
                {users.map((user) => (
                  <tr key={user.id}>
                    <td>{user.id}</td>
                    <td>{user.name || '—'}</td>
                    <td>{user.email}</td>
                    <td>{user.phone || '—'}</td>
                    <td>{user.role}</td>
                    <td>{user.status ?? 'Not tracked'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

export default ManageUsers;
