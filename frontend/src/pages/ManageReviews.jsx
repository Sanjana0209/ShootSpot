import { useCallback, useEffect, useState } from 'react';
import { fetchAdminReviews, removeAdminReview } from '../services/reviews';

function formatDate(value) {
  return new Date(value).toLocaleString('en-IN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

function ManageReviews() {
  const [reviews, setReviews] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [removingId, setRemovingId] = useState(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const loadReviews = useCallback(async () => {
    setError('');
    try {
      setReviews(await fetchAdminReviews());
    } catch (requestError) {
      setError(requestError.message || 'Unable to load reviews.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let isCurrent = true;
    fetchAdminReviews()
      .then((loadedReviews) => {
        if (isCurrent) setReviews(loadedReviews);
      })
      .catch((requestError) => {
        if (isCurrent) setError(requestError.message || 'Unable to load reviews.');
      })
      .finally(() => {
        if (isCurrent) setIsLoading(false);
      });
    return () => {
      isCurrent = false;
    };
  }, []);

  const handleRemove = async (review) => {
    if (!window.confirm(`Remove the review for ${review.locationName}? This cannot be undone.`)) {
      return;
    }
    setRemovingId(review.id);
    setError('');
    setMessage('');
    try {
      await removeAdminReview(review.id);
      setReviews((current) => current.filter((item) => item.id !== review.id));
      setMessage(`Review #${review.id} was removed.`);
    } catch (requestError) {
      setError(requestError.message || 'Unable to remove the review.');
    } finally {
      setRemovingId(null);
    }
  };

  return (
    <div className="admin-manage-page">
      <div className="dashboard-header">
        <div>
          <p className="eyebrow">Manage reviews</p>
          <h1>Reviews</h1>
        </div>
        <button type="button" className="btn btn-secondary small" onClick={() => {
          setIsLoading(true);
          loadReviews();
        }} disabled={isLoading}>
          Refresh
        </button>
      </div>

      {error && <p className="form-message form-message-error" role="alert">{error}</p>}
      {message && <p className="form-message" role="status">{message}</p>}

      <section className="panel-card table-panel">
        {isLoading ? (
          <p role="status">Loading reviews...</p>
        ) : reviews.length === 0 ? (
          <p>No customer reviews have been submitted yet.</p>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Customer</th>
                  <th>Location</th>
                  <th>Rating</th>
                  <th>Review</th>
                  <th>Date</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {reviews.map((review) => (
                  <tr key={review.id}>
                    <td>
                      <strong>{review.customerName}</strong>
                      <br />
                      <a href={`mailto:${review.customerEmail}`}>{review.customerEmail}</a>
                      <br />
                      {review.bookingId ? `Booking #${review.bookingId}` : 'Location review'}
                    </td>
                    <td>{review.locationName}</td>
                    <td>{review.rating}/5</td>
                    <td className="review-table-comment">{review.comment}</td>
                    <td>{formatDate(review.createdAt)}</td>
                    <td className="table-actions">
                      <button
                        type="button"
                        className="link-btn danger"
                        onClick={() => handleRemove(review)}
                        disabled={removingId === review.id}
                      >
                        {removingId === review.id ? 'Removing...' : 'Remove'}
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

export default ManageReviews;
