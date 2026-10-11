import { useState } from 'react';
import { users as initialUsers } from '../data/sampleData';

function ManageUsers() {
  const [users, setUsers] = useState(initialUsers);

  const handleToggleStatus = (userId) => {
    setUsers((prev) =>
      prev.map((user) =>
        user.id === userId
          ? { ...user, status: user.status === 'Active' ? 'Inactive' : 'Active' }
          : user,
      ),
    );
  };

  return (
    <div className="admin-manage-page">
      <div className="dashboard-header">
        <div>
          <p className="eyebrow">Manage users</p>
          <h1>Users</h1>
        </div>
      </div>

      <section className="panel-card table-panel">
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
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>{user.id}</td>
                  <td>{user.name}</td>
                  <td>{user.email}</td>
                  <td>{user.phone}</td>
                  <td>{user.role}</td>
                  <td>{user.status}</td>
                  <td className="table-actions">
                    <button type="button" className="link-btn">
                      View
                    </button>
                    <button type="button" className="link-btn" onClick={() => handleToggleStatus(user.id)}>
                      {user.status === 'Active' ? 'Deactivate' : 'Activate'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

export default ManageUsers;
