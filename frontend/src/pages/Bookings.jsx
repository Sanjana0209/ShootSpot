import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { clearAuthSession } from '../services/auth';
import { AuthenticationRequiredError, fetchMyBookings } from '../services/bookings';
import { submitBookingReview } from '../services/reviews';
import { fetchLocationById } from '../services/locations';
import { formatTime } from '../utils/time';

function formatDate(value) {
  return new Date(`${value}T00:00:00`).toLocaleDateString('en-IN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

function Bookings() {
  const navigate = useNavigate();
  const location = useLocation();
  const [bookings, setBookings] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [reviewingBookingId, setReviewingBookingId] = useState(null);
  const [reviewRating, setReviewRating] = useState('5');
  const [reviewComment, setReviewComment] = useState('');
  const [reviewError, setReviewError] = useState('');
  const [isSubmittingReview, setIsSubmittingReview] = useState(false);
  const [locationRatings, setLocationRatings] = useState({});

  useEffect(() => {
    let isCurrent = true;
    fetchMyBookings()
      .then((currentBookings) => {
        if (isCurrent) setBookings(currentBookings);
      })
      .catch((fetchError) => {
        if (!isCurrent) return;
        if (fetchError instanceof AuthenticationRequiredError) {
          clearAuthSession();
          navigate('/login', { replace: true });
          return;
        }
        setError(fetchError.message || 'Unable to load your bookings.');
      })
      .finally(() => {
        if (isCurrent) setIsLoading(false);
      });

    return () => {
      isCurrent = false;
    };
  }, [navigate]);

  const handleReviewSubmit = async (event, bookingId) => {
    event.preventDefault();
    setReviewError('');
    const comment = reviewComment.trim();
    if (comment.length < 3 || comment.length > 500) {
      setReviewError('Review must be between 3 and 500 characters.');
      return;
    }

    setIsSubmittingReview(true);
    try {
      await submitBookingReview(bookingId, {
        rating: Number(reviewRating),
        comment,
      });
      setBookings((previous) => previous.map((booking) => (
        booking.id === bookingId ? { ...booking, reviewed: true } : booking
      )));
      setReviewingBookingId(null);
      setReviewComment('');
      setMessage('Your review was submitted. Thank you for sharing your feedback.');
      const booking = bookings.find((item) => item.id === bookingId);
      try {
        const updatedLocation = await fetchLocationById(booking.locationId);
        setLocationRatings((previous) => ({
          ...previous,
          [booking.locationId]: {
            averageRating: updatedLocation.averageRating,
            reviewCount: updatedLocation.reviewCount,
          },
        }));
      } catch {
        setMessage('Your review was submitted. Reload the location page to see its updated rating.');
      }
    } catch (submitError) {
      if (submitError instanceof AuthenticationRequiredError) {
        clearAuthSession();
        navigate('/login', { replace: true });
        return;
      }
      setReviewError(submitError.message || 'Unable to submit your review.');
    } finally {
      setIsSubmittingReview(false);
    }
  };

  const hasCompletedBooking = bookings.some(
    (booking) => String(booking.status || '').trim().toUpperCase() === 'COMPLETED',
  );

  return (
    <div className="container section-block">
      <div className="section-heading">
        <p className="eyebrow">Bookings</p>
        <h1>My Bookings</h1>
      </div>

      {location.state?.message && (
        <p className="form-message" role="status">{location.state.message}</p>
      )}
      {message && <p className="form-message" role="status">{message}</p>}
      {!isLoading && !error && bookings.length > 0 && !hasCompletedBooking && (
        <p className="muted">
          You can review a location after your booking is marked COMPLETED.
        </p>
      )}

      {isLoading ? (
        <div className="empty-state" role="status"><h3>Loading your bookings...</h3></div>
      ) : error ? (
        <div className="empty-state" role="alert">
          <h3>Unable to load bookings.</h3>
          <p>{error}</p>
        </div>
      ) : bookings.length === 0 ? (
        <div className="empty-state">
          <h3>You have no bookings yet.</h3>
          <p>Browse locations and choose a date and time to reserve a space.</p>
          <Link to="/explore" className="btn btn-primary">Browse Locations</Link>
        </div>
      ) : (
        <div className="booking-list">
          {bookings.map((booking) => {
            const isCompleted = String(booking.status || '').trim().toUpperCase() === 'COMPLETED';
            return (
            <article key={booking.id} className="booking-card">
              <div className="booking-card-header">
                <div>
                  <p className="eyebrow">{booking.locationCity}</p>
                  <h2>{booking.locationName}</h2>
                </div>
                <span className={`status-badge ${booking.status.toLowerCase()}`}>
                  {booking.status}
                </span>
              </div>
              <div className="booking-card-details">
                <p><strong>Date:</strong> {formatDate(booking.bookingDate)}</p>
                <p><strong>Time:</strong> {formatTime(booking.startTime)}–{formatTime(booking.endTime)}</p>
                <p><strong>Duration:</strong> {booking.durationHours} {booking.durationHours === 1 ? 'hour' : 'hours'}</p>
                <p><strong>Total:</strong> ₹{Number(booking.totalPrice).toLocaleString('en-IN')}</p>
              </div>
              <Link to={`/locations/${booking.locationId}`} className="text-link">
                View location
              </Link>
              {locationRatings[booking.locationId] && (
                <p className="booking-location-rating" role="status">
                  Location rating: {locationRatings[booking.locationId].averageRating == null
                    ? 'No ratings yet'
                    : `⭐ ${Number(locationRatings[booking.locationId].averageRating).toFixed(1)} / 5`}
                  {' '}({locationRatings[booking.locationId].reviewCount} reviews)
                </p>
              )}
              {isCompleted && (
                booking.reviewed ? (
                  <p className="review-submitted" role="status">You have reviewed this completed booking.</p>
                ) : reviewingBookingId === booking.id ? (
                  <form
                    className="booking-review-form"
                    onSubmit={(event) => handleReviewSubmit(event, booking.id)}
                  >
                    <h3>Review your experience</h3>
                    <div className="form-group">
                      <label htmlFor={`review-rating-${booking.id}`}>Rating</label>
                      <select
                        id={`review-rating-${booking.id}`}
                        value={reviewRating}
                        onChange={(event) => setReviewRating(event.target.value)}
                        disabled={isSubmittingReview}
                      >
                        {[5, 4, 3, 2, 1].map((rating) => (
                          <option key={rating} value={rating}>
                            {rating} {rating === 1 ? 'star' : 'stars'}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="form-group">
                      <label htmlFor={`review-comment-${booking.id}`}>Your review</label>
                      <textarea
                        id={`review-comment-${booking.id}`}
                        value={reviewComment}
                        onChange={(event) => setReviewComment(event.target.value)}
                        minLength={3}
                        maxLength={500}
                        rows={4}
                        required
                        disabled={isSubmittingReview}
                      />
                      <small>{reviewComment.trim().length}/500 characters (minimum 3).</small>
                    </div>
                    {reviewError && (
                      <p className="form-message form-message-error" role="alert">{reviewError}</p>
                    )}
                    <div className="booking-review-actions">
                      <button type="submit" className="btn btn-primary small" disabled={isSubmittingReview}>
                        {isSubmittingReview ? 'Submitting...' : 'Submit review'}
                      </button>
                      <button
                        type="button"
                        className="btn btn-secondary small"
                        onClick={() => {
                          setReviewingBookingId(null);
                          setReviewError('');
                        }}
                        disabled={isSubmittingReview}
                      >
                        Cancel
                      </button>
                    </div>
                  </form>
                ) : (
                  <button
                    type="button"
                    className="btn btn-secondary small"
                    onClick={() => {
                      setReviewingBookingId(booking.id);
                      setReviewError('');
                    }}
                  >
                    Write Review
                  </button>
                )
              )}
            </article>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default Bookings;
