import { Link } from 'react-router-dom';
import LocationCard from '../components/LocationCard';
import useFavorites from '../hooks/useFavorites';

function Favorites() {
  const {
    favorites,
    favoriteIds,
    isLoading,
    error,
    busyLocationId,
    toggleFavorite,
  } = useFavorites();

  return (
    <div className="container section-block">
      <div className="section-heading">
        <p className="eyebrow">Saved spaces</p>
        <h1>Favorite Locations</h1>
      </div>

      {error && <p className="form-message form-message-error" role="alert">{error}</p>}

      <div className="location-grid">
        {isLoading ? (
          <div className="empty-state" role="status">
            <h3>Loading your favourites...</h3>
          </div>
        ) : error ? (
          <div className="empty-state" role="alert">
            <h3>Unable to load your favourites.</h3>
          </div>
        ) : favorites.length > 0 ? (
          favorites.map((location) => (
            <LocationCard
              key={location.id}
              location={location}
              isFavorite={favoriteIds.includes(String(location.id))}
              isFavoritePending={busyLocationId === String(location.id)}
              onToggleFavorite={toggleFavorite}
            />
          ))
        ) : (
          <div className="empty-state">
            <h3>No favorites yet.</h3>
            <p>Start saving locations you like from the explore page.</p>
            <Link to="/explore" className="btn btn-primary small">Explore locations</Link>
          </div>
        )}
      </div>
    </div>
  );
}

export default Favorites;
