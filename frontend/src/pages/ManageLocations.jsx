import { useCallback, useEffect, useState } from 'react';
import { fetchLocations } from '../services/locations';
import { deleteLocation, saveLocation } from '../services/admin';

const emptyForm = {
  name: '',
  address: '',
  city: '',
  category: 'Photoshoot',
  description: '',
  pricePerHour: '',
  imageUrl: '',
  available: true,
  bookable: false,
};

function ManageLocations() {
  const [locations, setLocations] = useState([]);
  const [formData, setFormData] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const loadLocations = useCallback(async () => {
    try {
      setLocations(await fetchLocations());
    } catch (requestError) {
      setError(requestError.message || 'Unable to load locations.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let isCurrent = true;
    fetchLocations()
      .then((loadedLocations) => {
        if (isCurrent) setLocations(loadedLocations);
      })
      .catch((requestError) => {
        if (isCurrent) setError(requestError.message || 'Unable to load locations.');
      })
      .finally(() => {
        if (isCurrent) setIsLoading(false);
      });
    return () => {
      isCurrent = false;
    };
  }, []);

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target;
    setFormData((previous) => ({
      ...previous,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const resetForm = () => {
    setFormData(emptyForm);
    setEditingId(null);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setMessage('');
    setIsSaving(true);

    try {
      await saveLocation({
        ...formData,
        name: formData.name.trim(),
        address: formData.address.trim(),
        city: formData.city.trim(),
        description: formData.description.trim(),
        pricePerHour: Number(formData.pricePerHour),
      }, editingId);
      setMessage(editingId ? 'Location updated.' : 'Location added.');
      resetForm();
      await loadLocations();
    } catch (requestError) {
      setError(requestError.message || 'Unable to save location.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleEdit = (location) => {
    setEditingId(location.id);
    setMessage('');
    setError('');
    setFormData({
      name: location.name || '',
      address: location.address || '',
      city: location.city || '',
      category: location.category || 'Photoshoot',
      description: location.description || '',
      pricePerHour: location.pricePerHour == null ? '' : String(location.pricePerHour),
      imageUrl: location.imageUrl || '',
      available: Boolean(location.available),
      bookable: Boolean(location.bookable),
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleDelete = async (location) => {
    if (!window.confirm(`Delete "${location.name}"? This cannot be undone.`)) return;
    setDeletingId(location.id);
    setError('');
    setMessage('');
    try {
      await deleteLocation(location.id);
      setMessage('Location deleted.');
      if (editingId === location.id) resetForm();
      await loadLocations();
    } catch (requestError) {
      setError(requestError.message || 'Unable to delete location.');
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div className="admin-manage-page">
      <div className="dashboard-header">
        <div>
          <p className="eyebrow">Manage locations</p>
          <h1>Locations</h1>
        </div>
      </div>

      {error && <p className="form-message form-message-error" role="alert">{error}</p>}
      {message && <p className="form-message" role="status">{message}</p>}

      <section className="panel-card form-panel">
        <h2>{editingId ? 'Edit Location' : 'Add Location'}</h2>
        <form onSubmit={handleSubmit} className="admin-form">
          <div className="form-grid two-col">
            <div className="form-group">
              <label htmlFor="locationName">Location name</label>
              <input id="locationName" name="name" value={formData.name} onChange={handleChange} required maxLength="120" />
            </div>
            <div className="form-group">
              <label htmlFor="locationAddress">Address</label>
              <input id="locationAddress" name="address" value={formData.address} onChange={handleChange} required maxLength="255" />
            </div>
            <div className="form-group">
              <label htmlFor="locationCity">City</label>
              <input id="locationCity" name="city" value={formData.city} onChange={handleChange} required maxLength="100" />
            </div>
            <div className="form-group">
              <label htmlFor="locationCategory">Category</label>
              <select id="locationCategory" name="category" value={formData.category} onChange={handleChange} required>
                <option value="Photoshoot">Photoshoot</option>
                <option value="Reels">Reels</option>
                <option value="Short Film">Short Film</option>
                <option value="Advertisement">Advertisement</option>
                <option value="Pre-wedding">Pre-wedding</option>
              </select>
            </div>
            <div className="form-group">
              <label htmlFor="locationPrice">Price per hour</label>
              <input
                id="locationPrice"
                name="pricePerHour"
                type="number"
                min="0.01"
                step="0.01"
                value={formData.pricePerHour}
                onChange={handleChange}
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="locationImage">Image URL</label>
              <input id="locationImage" name="imageUrl" type="url" value={formData.imageUrl} onChange={handleChange} maxLength="2048" />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="locationDescription">Description</label>
            <textarea
              id="locationDescription"
              name="description"
              rows="4"
              value={formData.description}
              onChange={handleChange}
              maxLength="2000"
            />
          </div>

          <label className="checkbox-row">
            <input type="checkbox" name="available" checked={formData.available} onChange={handleChange} />
            Available for booking
          </label>
          <label className="checkbox-row">
            <input type="checkbox" name="bookable" checked={formData.bookable} onChange={handleChange} />
            Verified and bookable
          </label>
          <p className="muted">
            Only enable this after verifying the venue and its permission to host bookings.
            New and existing locations remain non-bookable until explicitly enabled.
          </p>

          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={isSaving}>
              {isSaving ? 'Saving...' : editingId ? 'Update Location' : 'Add Location'}
            </button>
            {editingId && (
              <button type="button" className="btn btn-secondary" onClick={resetForm} disabled={isSaving}>
                Cancel edit
              </button>
            )}
          </div>
        </form>
      </section>

      <section className="panel-card table-panel">
        <h2>Location list</h2>
        {isLoading ? (
          <p role="status">Loading locations...</p>
        ) : locations.length === 0 ? (
          <p>No locations are available.</p>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>City</th>
                  <th>Category</th>
                  <th>Price / hour</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {locations.map((location) => (
                  <tr key={location.id}>
                    <td>{location.name}</td>
                    <td>{location.city}</td>
                    <td>{location.category}</td>
                    <td>₹{Number(location.pricePerHour).toLocaleString('en-IN')}</td>
                    <td>{location.bookable ? 'Bookable' : 'Not bookable'} · {location.available ? 'Available' : 'Unavailable'}</td>
                    <td className="table-actions">
                      <button type="button" className="link-btn" onClick={() => handleEdit(location)}>
                        Edit
                      </button>
                      <button
                        type="button"
                        className="link-btn danger"
                        onClick={() => handleDelete(location)}
                        disabled={deletingId === location.id}
                      >
                        {deletingId === location.id ? 'Deleting...' : 'Delete'}
                      </button>
                    </td>
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

export default ManageLocations;
