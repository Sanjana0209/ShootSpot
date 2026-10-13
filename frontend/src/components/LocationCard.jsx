import { Link } from 'react-router-dom';
import LocationImage from './LocationImage';

function LocationCard({
  location,
  isFavorite = false,
  onToggleFavorite,
  isFavoritePending = false,
}) {
  const locationId = String(location.id ?? '');
  const hasDatabaseId = /^\d+$/.test(locationId) && !/^0+$/.test(locationId);
  const detailsPath = hasDatabaseId ? `/locations/${locationId}` : '/explore';

  return (
    <article className="location-card">
      <div className="card-image-wrap">
        <LocationImage
          key={location.imageUrl || location.image || 'no-image'}
          src={location.imageUrl || location.image}
          alt={location.name}
          linkTo={detailsPath}
        />
        <button
          type="button"
          className={`favorite-button ${isFavorite ? 'active' : ''}`}
          onClick={() => onToggleFavorite(location)}
          disabled={isFavoritePending}
          aria-label={isFavorite ? 'Remove favorite' : 'Add favorite'}
          aria-pressed={isFavorite}
        >
          {isFavorite ? '♥' : '♡'}
        </button>
      </div>

      <div className="location-card-body">
        <p className={`location-bookability ${location.bookable ? 'bookable' : 'not-bookable'}`}>
          {location.bookable
            ? location.available === false ? 'Verified · currently unavailable' : 'Verified · bookable'
            : 'Not verified for booking'}
        </p>
        <div className="location-card-header">
          <div>
            <h3><Link to={detailsPath}>{location.name}</Link></h3>
            <p className="muted">{location.city}</p>
          </div>
          <span className="category-tag">{location.category}</span>
        </div>

        <div className="location-meta">
          <span>
            ⭐ {location.averageRating !== undefined
              ? location.averageRating == null ? 'New' : Number(location.averageRating).toFixed(1)
              : location.rating}
            {location.reviewCount !== undefined && ` (${location.reviewCount} reviews)`}
          </span>
          <span>₹{location.pricePerHour}/hr</span>
        </div>

        <div className="card-actions">
          <Link to={detailsPath} className="btn btn-primary small">
            {hasDatabaseId ? 'View Details' : 'Browse Locations'}
          </Link>
        </div>
      </div>
    </article>
  );
}

export default LocationCard;
