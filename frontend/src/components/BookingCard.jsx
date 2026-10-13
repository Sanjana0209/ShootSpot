import { formatTime } from '../utils/time';

function BookingCard({ booking, onCancel }) {
  const canCancel = booking.status !== 'Cancelled' && booking.status !== 'Completed';

  return (
    <article className="booking-card">
      <div className="booking-main">
        <div>
          <p className="label">Booking ID</p>
          <h3>{booking.id}</h3>
        </div>
        <span className={`status-badge ${booking.status.toLowerCase()}`}>{booking.status}</span>
      </div>

      <div className="booking-details">
        <div>
          <p className="label">Location</p>
          <strong>{booking.location}</strong>
        </div>
        <div>
          <p className="label">Date</p>
          <strong>{booking.date}</strong>
        </div>
        <div>
          <p className="label">Time</p>
          <strong>
            {formatTime(booking.startTime)} - {formatTime(booking.endTime)}
          </strong>
        </div>
        <div>
          <p className="label">Total</p>
          <strong>₹{booking.totalAmount}</strong>
        </div>
      </div>

      {canCancel && (
        <button type="button" className="btn btn-secondary small" onClick={() => onCancel(booking.id)}>
          Cancel Booking
        </button>
      )}
    </article>
  );
}

export default BookingCard;
