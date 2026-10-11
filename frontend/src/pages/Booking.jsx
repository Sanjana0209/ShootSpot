import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { clearAuthSession } from '../services/auth';
import {
  AuthenticationRequiredError,
  BookingConflictError,
  fetchBookingAvailability,
  submitBooking,
} from '../services/bookings';
import { getAuthToken } from '../services/auth';
import { fetchLocationById, LocationNotFoundError } from '../services/locations';
import {
  addHoursToTime,
  formatTime,
  overlapsTimeInterval,
  parseTimeTo12Hour,
  to24HourTime,
} from '../utils/time';

function getLocalDateString() {
  const today = new Date();
  const timezoneOffset = today.getTimezoneOffset() * 60_000;
  return new Date(today.getTime() - timezoneOffset).toISOString().slice(0, 10);
}

function Booking() {
  const { id } = useParams();
  const routeLocation = useLocation();
  const navigate = useNavigate();
  const [location, setLocation] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [bookingDate, setBookingDate] = useState(
    () => routeLocation.state?.bookingDraft?.bookingDate || '',
  );
  const [hour, setHour] = useState(() => parseTimeTo12Hour(
    routeLocation.state?.bookingDraft?.startTime || '',
  ).hour);
  const [minute, setMinute] = useState(() => parseTimeTo12Hour(
    routeLocation.state?.bookingDraft?.startTime || '',
  ).minute);
  const [period, setPeriod] = useState(() => (
    routeLocation.state?.bookingDraft?.startTime
      ? parseTimeTo12Hour(routeLocation.state.bookingDraft.startTime).period
      : ''
  ));
  const [durationHours, setDurationHours] = useState(
    () => routeLocation.state?.bookingDraft?.durationHours || '1',
  );
  const [availabilityResult, setAvailabilityResult] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const startTime = to24HourTime(hour, minute, period);
  const requestedEndTime = addHoursToTime(startTime, durationHours);
  const minuteOptions = ['00', '15', '30', '45'];

  useEffect(() => {
    const controller = new AbortController();

    fetchLocationById(id, controller.signal)
      .then(setLocation)
      .catch((fetchError) => {
        if (fetchError.name !== 'AbortError') {
          setError(fetchError instanceof LocationNotFoundError
            ? fetchError.message
            : fetchError.message || 'Unable to load this location.');
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false);
      });

    return () => controller.abort();
  }, [id]);

  const availabilityKey = getAuthToken()
      && location?.bookable
      && location.available
      && bookingDate
      && startTime
      && durationHours
    ? JSON.stringify([id, bookingDate, startTime, durationHours])
    : '';
  const availability = availabilityResult?.key === availabilityKey
    ? availabilityResult.data
    : null;
  const availabilityError = availabilityResult?.key === availabilityKey
    ? availabilityResult.error
    : '';
  const isCheckingAvailability = Boolean(availabilityKey && availabilityResult?.key !== availabilityKey);
  const selectedSlotOverlaps = Boolean(
    availability
    && requestedEndTime
    && Array.isArray(availability.occupiedIntervals)
    && availability.occupiedIntervals.some((interval) => overlapsTimeInterval(
      startTime,
      requestedEndTime,
      interval.startTime,
      interval.endTime,
    )),
  );

  useEffect(() => {
    if (!availabilityKey) return undefined;
    const controller = new AbortController();
    fetchBookingAvailability({
      locationId: Number(id),
      bookingDate,
      startTime,
      durationHours: Number(durationHours),
      signal: controller.signal,
    })
      .then((data) => setAvailabilityResult({ key: availabilityKey, data, error: '' }))
      .catch((availabilityFetchError) => {
        if (availabilityFetchError.name === 'AbortError') return;
        if (availabilityFetchError instanceof AuthenticationRequiredError) {
          clearAuthSession();
          navigate('/login', {
            replace: true,
            state: {
              returnTo: routeLocation.pathname,
              bookingDraft: { bookingDate, startTime, durationHours },
            },
          });
          return;
        }
        setAvailabilityResult({
          key: availabilityKey,
          data: null,
          error: availabilityFetchError.message
            || 'Unable to check this time slot. The server will still verify it when you submit.',
        });
      });

    return () => controller.abort();
  }, [availabilityKey, bookingDate, durationHours, id, navigate, routeLocation.pathname, startTime]);

  const estimatedTotal = useMemo(() => {
    if (!location?.pricePerHour || !durationHours) return null;
    return Number(location.pricePerHour) * Number(durationHours);
  }, [location, durationHours]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (selectedSlotOverlaps) {
      setError('Already booked for this time slot. Please choose another time.');
      return;
    }
    setError('');
    setIsSubmitting(true);

    try {
      await submitBooking({
        locationId: Number(id),
        bookingDate,
        startTime,
        durationHours: Number(durationHours),
      });
      navigate('/bookings', {
        replace: true,
        state: { message: 'Your booking request was submitted and is awaiting admin confirmation.' },
      });
    } catch (submitError) {
      if (submitError instanceof AuthenticationRequiredError) {
        clearAuthSession();
        navigate('/login', {
          replace: true,
          state: {
            returnTo: routeLocation.pathname,
            bookingDraft: { bookingDate, startTime, durationHours },
          },
        });
        return;
      }
      if (submitError instanceof BookingConflictError) {
        const isSlotConflict = /already booked|overlaps an existing/i.test(submitError.message);
        setError(isSlotConflict
          ? 'Already booked for this time slot. Please choose another time.'
          : submitError.message);
        if (availabilityKey) {
          try {
            const refreshedAvailability = await fetchBookingAvailability({
              locationId: Number(id),
              bookingDate,
              startTime,
              durationHours: Number(durationHours),
            });
            setAvailabilityResult({
              key: availabilityKey,
              data: refreshedAvailability,
              error: '',
            });
          } catch (availabilityRefreshError) {
            if (availabilityRefreshError instanceof AuthenticationRequiredError) {
              clearAuthSession();
              navigate('/login', {
                replace: true,
                state: {
                  returnTo: routeLocation.pathname,
                  bookingDraft: { bookingDate, startTime, durationHours },
                },
              });
              return;
            }
            setAvailabilityResult({
              key: availabilityKey,
              data: null,
              error: availabilityRefreshError.message
                || 'Could not refresh availability. Please choose another time and try again.',
            });
          }
        }
        return;
      }
      setError(submitError.message || 'Unable to create your booking.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return <main className="container section-block" role="status">Loading booking details...</main>;
  }

  if (error && !location) {
    return (
      <main className="container section-block">
        <div className="empty-state" role="alert">
          <h1>Unable to book this location</h1>
          <p>{error}</p>
          <Link to="/explore" className="btn btn-secondary">Browse locations</Link>
        </div>
      </main>
    );
  }

  if (!location) return null;

  return (
    <div className="container section-block">
      <Link to={`/locations/${encodeURIComponent(id)}`} className="back-link">
        ← Back to {location.name}
      </Link>
      <div className="section-heading">
        <p className="eyebrow">Reserve a location</p>
        <h1>Book {location.name}</h1>
        <p className="muted">{location.city}</p>
      </div>

      {!location.available && (
        <div className="form-message form-message-error" role="alert">
          This location is currently unavailable for booking.
        </div>
      )}
      {!location.bookable && (
        <div className="form-message form-message-error" role="alert">
          This sample or unverified listing is not bookable. An administrator must verify it
          before booking can be enabled.
        </div>
      )}


      <form className="booking-form panel-card" onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="bookingDate">Booking date</label>
          <input
            id="bookingDate"
            type="date"
            min={getLocalDateString()}
            value={bookingDate}
            onChange={(event) => {
              setBookingDate(event.target.value);
              setError('');
            }}
            required
            disabled={!location.available || !location.bookable || isSubmitting}
          />
        </div>

        <div className="form-group">
          <label htmlFor="startTime">Start time</label>
          <div className="time-selector">
            <label>
              Hour
              <select
                aria-label="Start time hour"
                value={hour}
                onChange={(event) => {
                  setHour(event.target.value);
                  setError('');
                }}
                required
                disabled={!location.available || !location.bookable || isSubmitting}
              >
                <option value="" disabled>Select</option>
                {Array.from({ length: 12 }, (_, index) => index + 1).map((hour) => (
                  <option key={hour} value={hour}>{hour}</option>
                ))}
              </select>
            </label>
            <label>
              Minutes
              <select
                aria-label="Start time minutes"
                value={minute}
                onChange={(event) => {
                  setMinute(event.target.value);
                  setError('');
                }}
                required
                disabled={!location.available || !location.bookable || isSubmitting}
              >
                <option value="" disabled>Select</option>
                {minuteOptions.map((optionMinute) => (
                  <option key={optionMinute} value={optionMinute}>{optionMinute}</option>
                ))}
              </select>
            </label>
            <label>
              AM/PM
              <select
                aria-label="Start time AM or PM"
                value={period}
                onChange={(event) => {
                  setPeriod(event.target.value);
                  setError('');
                }}
                required
                disabled={!location.available || !location.bookable || isSubmitting}
              >
                <option value="" disabled>Select</option>
                <option value="AM">AM</option>
                <option value="PM">PM</option>
              </select>
            </label>
          </div>
          {!startTime && (
            <small className="time-format-hint">
              Select an hour, minutes, and AM or PM to complete the start time.
            </small>
          )}
          {startTime && (
            <small className="time-format-hint">
              Selected start time: {formatTime(startTime)}
            </small>
          )}
        </div>

        <div className="form-group">
          <label htmlFor="durationHours">Duration</label>
          <select
            id="durationHours"
            value={durationHours}
            onChange={(event) => {
              setDurationHours(event.target.value);
              setError('');
            }}
            required
            disabled={!location.available || !location.bookable || isSubmitting}
          >
            {Array.from({ length: 12 }, (_, index) => index + 1).map((hours) => (
              <option key={hours} value={hours}>
                {hours} {hours === 1 ? 'hour' : 'hours'}
              </option>
            ))}
          </select>
        </div>

        {startTime && requestedEndTime && (
          <p className="time-format-hint">
            Requested end time: {formatTime(requestedEndTime)}
          </p>
        )}
        {startTime && !requestedEndTime && (
          <p className="form-message form-message-error" role="alert">
            A booking must end before midnight. Choose an earlier start time or shorter duration.
          </p>
        )}

        {bookingDate && startTime && durationHours && location.bookable && location.available && (
          <section className="booking-availability panel-card" aria-live="polite">
            <h2>Selected date availability</h2>
            {isCheckingAvailability ? (
              <p role="status">Checking occupied times...</p>
            ) : availabilityError ? (
              <p className="form-message form-message-error" role="alert">{availabilityError}</p>
            ) : availability ? (
              <>
                {availability.occupiedIntervals.length > 0 ? (
                  <ul>
                    {availability.occupiedIntervals.map((interval) => (
                      <li key={`${interval.startTime}-${interval.endTime}`}>
                        Already occupied: {formatTime(interval.startTime)} – {formatTime(interval.endTime)}
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p>No pending or confirmed bookings currently occupy this date.</p>
                )}
                {availability.requestedSlotAvailable ? (
                  <p className="availability-confirmation">
                    Your selected time is currently available for {durationHours} {Number(durationHours) === 1 ? 'hour' : 'hours'}.
                  </p>
                ) : selectedSlotOverlaps ? (
                  <p className="form-message form-message-error" role="alert">
                    Already booked for this time slot. Please choose another time.
                  </p>
                ) : availability.nextAvailableTime ? (
                  <p>
                    Available from {formatTime(availability.nextAvailableTime)} for the requested duration.
                  </p>
                ) : (
                  <p>No suitable time remains on this date. Please choose another time or date.</p>
                )}
              </>
            ) : null}
            <small>Availability is informational; the server checks again when you submit.</small>
          </section>
        )}

        <div className="booking-price-summary" aria-live="polite">
          <span>Hourly price</span>
          <strong>₹{Number(location.pricePerHour).toLocaleString('en-IN')}/hr</strong>
          <span>Estimated total</span>
          <strong>
            {estimatedTotal == null ? 'Price unavailable' : `₹${estimatedTotal.toLocaleString('en-IN')}`}
          </strong>
          <small>The final amount is calculated by the server using the stored location price.</small>
        </div>

        {error && <p className="form-message form-message-error" role="alert">{error}</p>}

        <button
          type="submit"
          className="btn btn-primary full-width"
          disabled={
            !location.available
            || !location.bookable
            || isSubmitting
            || !location.pricePerHour
            || !startTime
            || isCheckingAvailability
            || selectedSlotOverlaps
            || (startTime && !requestedEndTime)
          }
        >
          {isSubmitting ? 'Submitting booking...' : 'Request Booking'}
        </button>
      </form>
    </div>
  );
}

export default Booking;
