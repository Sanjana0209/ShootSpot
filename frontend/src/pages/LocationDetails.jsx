import { useEffect, useState } from 'react';
import { Link, useLocation as useRouterLocation, useNavigate, useParams } from 'react-router-dom';
import LocationImage from '../components/LocationImage';
import { getAuthUser } from '../services/auth';
import { fetchLocationById, LocationNotFoundError } from '../services/locations';
import { AuthenticationRequiredError } from '../services/bookings';
import { fetchLocationReviews, submitLocationReview } from '../services/reviews';

function LocationDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const routeLocation = useRouterLocation();
  const [location, setLocation] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [isNotFound, setIsNotFound] = useState(false);
  const [reviews, setReviews] = useState([]);
  const [reviewsLoading, setReviewsLoading] = useState(true);
  const [reviewsError, setReviewsError] = useState('');
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [reviewSubmitError, setReviewSubmitError] = useState('');
  const [reviewSubmitMessage, setReviewSubmitMessage] = useState('');
  const [isSubmittingReview, setIsSubmittingReview] = useState(false);
  const [hasSubmittedLocationReview, setHasSubmittedLocationReview] = useState(false);
  const currentUser = getAuthUser();

  useEffect(() => {
    const controller = new AbortController();

    const fetchLocation = async () => {
      setIsLoading(true);
      setError('');
      setIsNotFound(false);
      setLocation(null);
      setReviews([]);
      setReviewsLoading(true);
      setReviewsError('');

      try {
        setLocation(await fetchLocationById(id, controller.signal));
      } catch (fetchError) {
        if (fetchError instanceof LocationNotFoundError) {
          setIsNotFound(true);
        } else if (fetchError.name !== 'AbortError') {
          setError(fetchError.message || 'Unable to load this location.');
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    };

    fetchLocation();
    fetchLocationReviews(id, controller.signal)
      .then((loadedReviews) => {
        if (!controller.signal.aborted) setReviews(loadedReviews);
      })
      .catch((fetchError) => {
        if (!controller.signal.aborted) {
          setReviewsError(fetchError.message || 'Unable to load reviews.');
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setReviewsLoading(false);
      });
    return () => controller.abort();
  }, [id]);

  const handleReviewSubmit = async (event) => {
    event.preventDefault();
    setReviewSubmitError('');
    setReviewSubmitMessage('');
    const comment = reviewComment.trim();
    if (comment.length < 3 || comment.length > 500) {
      setReviewSubmitError('Your review must be between 3 and 500 characters.');
      return;
    }

    setIsSubmittingReview(true);
    try {
      const createdReview = await submitLocationReview(id, {
        rating: reviewRating,
        comment,
      });
      setReviews((existing) => [createdReview, ...existing]);
      setHasSubmittedLocationReview(true);
      setReviewComment('');
      setReviewRating(5);
      setReviewSubmitMessage('Thanks! Your review has been published.');
      try {
        setLocation(await fetchLocationById(id));
      } catch {
        setReviewSubmitMessage(
          'Thanks! Your review was published. Reload the page to see the updated rating summary.',
        );
      }
    } catch (submitError) {
      if (submitError instanceof AuthenticationRequiredError) {
        navigate('/login', {
          replace: true,
          state: { returnTo: routeLocation.pathname },
        });
        return;
      }
      setReviewSubmitError(submitError.message || 'Unable to submit your review.');
    } finally {
      setIsSubmittingReview(false);
    }
  };

  if (isLoading) {
    return (
      <main className="container section-block details-page" role="status">
        <p className="empty-state">Loading location details...</p>
      </main>
    );
  }

  if (isNotFound) {
    return (
      <main className="container section-block details-page">
        <div className="empty-state" role="status">
          <h1>Location not found</h1>
          <p>This location may have been removed or the link may be incorrect.</p>
          <Link to="/explore" className="btn btn-primary">Browse locations</Link>
        </div>
      </main>
    );
  }

  if (error) {
    return (
      <main className="container section-block details-page">
        <div className="empty-state" role="alert">
          <h1>Unable to load location</h1>
          <p>{error}</p>
          <Link to="/explore" className="btn btn-secondary">Back to Explore</Link>
        </div>
      </main>
    );
  }

  if (!location) return null;

  const image = location.imageUrl || location.image;
  const hasPrice = location.pricePerHour != null && location.pricePerHour !== '';
  const address = [location.address, location.city].filter(Boolean).join(', ');

  return (
    <div className="container section-block details-page">
      <Link to="/explore" className="back-link">
        ← Back to Explore
      </Link>

      <div className="details-layout">
        <div className="details-image-box">
          <LocationImage
            key={image || 'no-image'}
            src={image}
            alt={location.name || 'Location'}
            details
            showCredit
          />
        </div>

        <section className="details-content">
          {location.category && <p className="eyebrow">{location.category}</p>}
          {location.name && <h1>{location.name}</h1>}
          <div className="demo-listing-notice" role="note">
            {location.bookable
              ? 'Booking requests remain pending until confirmed. Verify venue access and filming permission before your shoot.'
              : 'This sample or unverified listing is not bookable. An administrator must verify the venue before enabling booking.'}
          </div>

          {(address || hasPrice) && (
            <div className="details-meta">
              {address && <span>📍 {address}</span>}
              {hasPrice && <span>₹{location.pricePerHour}/hr</span>}
            </div>
          )}

          {location.description && (
            <p className="details-description">{location.description}</p>
          )}

          {typeof location.available === 'boolean' && (
            <div className="availability-box">
              <h2>Availability</h2>
              <p>{location.available ? 'Available for booking' : 'Currently unavailable'}</p>
            </div>
          )}

          <div className="location-rating-summary" aria-label="Location rating">
            <strong>
              {location.averageRating == null
                ? 'No ratings yet'
                : `⭐ ${Number(location.averageRating).toFixed(1)} / 5`}
            </strong>
            <span>{location.reviewCount ?? 0} reviews</span>
          </div>

          <div className="details-actions">
            {!location.bookable ? (
              <button type="button" className="btn btn-primary" disabled>
                Not Bookable
              </button>
            ) : location.available === false ? (
              <button type="button" className="btn btn-primary" disabled>
                Currently Unavailable
              </button>
            ) : (
              <Link
                to={`/locations/${encodeURIComponent(id)}/book`}
                state={{ locationName: location.name }}
                className="btn btn-primary"
              >
                Book This Location
              </Link>
            )}
          </div>
        </section>
      </div>

      <section className="location-reviews" aria-labelledby="reviews-heading">
        <div className="section-heading">
          <p className="eyebrow">Customer feedback</p>
          <h2 id="reviews-heading">Reviews</h2>
        </div>
        {reviewSubmitMessage && (
          <p className="form-message" role="status">{reviewSubmitMessage}</p>
        )}
        {reviewSubmitError && (
          <p className="form-message form-message-error" role="alert">{reviewSubmitError}</p>
        )}
        {currentUser?.role === 'USER' && hasSubmittedLocationReview ? (
          <p className="review-submitted" role="status">Your review has been submitted. Thank you!</p>
        ) : currentUser?.role === 'USER' ? (
          <form className="location-review-form" onSubmit={handleReviewSubmit}>
            <h3>Share your experience</h3>
            <fieldset className="review-rating-field" disabled={isSubmittingReview}>
              <legend>Your rating</legend>
              <div className="review-stars" role="group" aria-label="Choose a rating from 1 to 5 stars">
                {[1, 2, 3, 4, 5].map((rating) => (
                  <button
                    key={rating}
                    type="button"
                    aria-pressed={reviewRating === rating}
                    aria-label={`${rating} ${rating === 1 ? 'star' : 'stars'}`}
                    className={rating <= reviewRating ? 'selected' : ''}
                    onClick={() => setReviewRating(rating)}
                  >
                    ★
                  </button>
                ))}
              </div>
            </fieldset>
            <label htmlFor="location-review-comment">Your review</label>
            <textarea
              id="location-review-comment"
              value={reviewComment}
              onChange={(event) => setReviewComment(event.target.value)}
              minLength={3}
              maxLength={500}
              rows={4}
              required
              disabled={isSubmittingReview}
            />
            <small>{reviewComment.trim().length}/500 characters (minimum 3).</small>
            <button type="submit" className="btn btn-primary" disabled={isSubmittingReview}>
              {isSubmittingReview ? 'Submitting review...' : 'Submit review'}
            </button>
          </form>
        ) : currentUser ? (
          <p className="muted">Reviews can be submitted by signed-in user accounts.</p>
        ) : (
          <p className="muted">
            <Link to="/login" state={{ returnTo: routeLocation.pathname }}>Sign in</Link>
            {' '}with a user account to leave a rating and review.
          </p>
        )}
        {reviewsLoading ? (
          <p role="status">Loading reviews...</p>
        ) : reviewsError ? (
          <p className="form-message form-message-error" role="alert">{reviewsError}</p>
        ) : reviews.length === 0 ? (
          <p className="muted">No reviews have been submitted for this location yet.</p>
        ) : (
          <div className="review-list">
            {reviews.map((review) => (
              <article className="review-card" key={review.id}>
                <div className="review-card-header">
                  <strong>{review.authorName || 'ShootSpot user'}</strong>
                  <span aria-label={`${review.rating} out of 5 stars`}>
                    {'★'.repeat(review.rating)}{'☆'.repeat(5 - review.rating)}
                  </span>
                </div>
                <p>{review.comment}</p>
                <time dateTime={review.createdAt}>
                  {new Date(review.createdAt).toLocaleDateString('en-IN', {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric',
                  })}
                </time>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export default LocationDetails;
