import { useCallback, useEffect, useState } from 'react';
import { fetchAdminBookings, updateAdminBookingStatus } from '../services/admin';
import { formatTime } from '../utils/time';

function formatDate(value) {
  return new Date(`${value}T00:00:00`).toLocaleDateString('en-IN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

function ManageBookings() {
  const [bookings, setBookings] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const loadBookings = useCallback(async () => {
    try {
      setBookings(await fetchAdminBookings());
    } catch (requestError) {
      setError(requestError.message || 'Unable to load bookings.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let isCurrent = true;
    fetchAdminBookings()
      .then((loadedBookings) => {
        if (isCurrent) setBookings(loadedBookings);
      })
      .catch((requestError) => {
        if (isCurrent) setError(requestError.message || 'Unable to load bookings.');
      })
      .finally(() => {
        if (isCurrent) setIsLoading(false);
      });
    return () => {
      isCurrent = false;
    };
  }, []);

  const handleStatusChange = async (bookingId, status) => {
    setUpdatingId(bookingId);
    setError('');
    setMessage('');
    try {
      const updatedBooking = await updateAdminBookingStatus(bookingId, status);
      setBookings((previous) => previous.map((booking) => (
        booking.id === bookingId ? updatedBooking : booking
      )));
      setMessage(`Booking #${bookingId} updated to ${status.toLowerCase()}.`);
      try {
        setBookings(await fetchAdminBookings());
      } catch (refreshError) {
        setError(`Status was saved, but the booking list could not be refreshed: ${refreshError.message}`);
      }
    } catch (requestError) {
      setError(requestError.message || 'Unable to update booking status.');
    } finally {
      setUpdatingId(null);
    }
  };

  const handleRefresh = () => {
    setIsLoading(true);
    setError('');
    loadBookings();
  };

  return (
    <div className="admin-manage-page">
      <div className="dashboard-header">
        <div>
          <p className="eyebrow">Manage bookings</p>
          <h1>Bookings</h1>
        </div>
        <button type="button" className="btn btn-secondary small" onClick={handleRefresh} disabled={isLoading}>
          Refresh
        </button>
      </div>

      {error && <p className="form-message form-message-error" role="alert">{error}</p>}
      {message && <p className="form-message" role="status">{message}</p>}

      <section className="panel-card table-panel">
        {isLoading ? (
          <p role="status">Loading customer bookings...</p>
        ) : bookings.length === 0 ? (
          <p>No bookings have been created yet.</p>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Booking</th>
                  <th>Customer</th>
                  <th>Location</th>
                  <th>Date</th>
                  <th>Time</th>
                  <th>Price</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {bookings.map((booking) => {
                  const canConfirm = booking.status === 'PENDING';
                  const canCancel = booking.status === 'PENDING' || booking.status === 'CONFIRMED';
                  return (
                    <tr key={booking.id}>
                      <td>#{booking.id}</td>
                      <td>
                        <strong>{booking.customerName}</strong>
                        <br />
                        <a href={`mailto:${booking.customerEmail}`}>{booking.customerEmail}</a>
                        {booking.customerPhone && <><br />{booking.customerPhone}</>}
                      </td>
                      <td>
                        <strong>{booking.locationName}</strong>
                        {booking.locationCity && <><br />{booking.locationCity}</>}
                      </td>
                      <td>{formatDate(booking.bookingDate)}</td>
                      <td>{formatTime(booking.startTime)}–{formatTime(booking.endTime)}<br />{booking.durationHours} hr</td>
                      <td>₹{Number(booking.totalPrice).toLocaleString('en-IN')}</td>
                      <td>
                        {canConfirm || canCancel ? (
                          <div className="booking-admin-actions">
                            <span className={`status-badge ${booking.status.toLowerCase()}`}>
                              {booking.status}
                            </span>
                            {canConfirm && (
                              <button
                                type="button"
                                className="btn btn-primary small"
                                onClick={() => handleStatusChange(booking.id, 'CONFIRMED')}
                                disabled={updatingId === booking.id}
                              >
                                Confirm
                              </button>
                            )}
                            {canCancel && (
                              <button
                                type="button"
                                className="btn btn-secondary small"
                                onClick={() => handleStatusChange(booking.id, 'CANCELLED')}
                                disabled={updatingId === booking.id}
                              >
                                Cancel
                              </button>
                            )}
                          </div>
                        ) : (
                          <span className={`status-badge ${booking.status.toLowerCase()}`}>
                            {booking.status}
                          </span>
                        )}
                        {updatingId === booking.id && <small className="admin-inline-status">Saving...</small>}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

export default ManageBookings;
