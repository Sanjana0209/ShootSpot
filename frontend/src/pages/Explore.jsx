import { useEffect, useMemo, useState } from 'react';
import LocationCard from '../components/LocationCard';
import SearchBar from '../components/SearchBar';
import useFavorites from '../hooks/useFavorites';
import { fetchLocations } from '../services/locations';

function Explore() {
  const [locations, setLocations] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const { favoriteIds, error: favoritesError, busyLocationId, toggleFavorite } = useFavorites();
  const [searchData, setSearchData] = useState({
    location: '',
    shootType: 'All',
    date: '',
  });
  const [selectedCity, setSelectedCity] = useState('All');
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');

  useEffect(() => {
    const controller = new AbortController();

    const loadLocations = async () => {
      try {
        setLocations(await fetchLocations(controller.signal));
      } catch (fetchError) {
        if (fetchError.name !== 'AbortError') {
          setError(fetchError.message || 'Unable to load locations.');
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    };

    loadLocations();
    return () => controller.abort();
  }, []);

  const cityOptions = useMemo(
    () => ['All', ...new Set(locations.map((location) => location.city).filter(Boolean))],
    [locations],
  );
  const categoryOptions = useMemo(
    () => ['All', ...new Set(locations.map((location) => location.category).filter(Boolean))],
    [locations],
  );

  const filteredLocations = useMemo(() => {
    const searchTerm = searchData.location.trim().toLocaleLowerCase();
    const minimum = minPrice === '' ? null : Number(minPrice);
    const maximum = maxPrice === '' ? null : Number(maxPrice);
    if (minimum !== null && maximum !== null && minimum > maximum) {
      return [];
    }

    return locations.filter((location) => {
      const price = Number(location.pricePerHour);
      const matchesSearch =
        !searchTerm ||
        (location.name || '').toLocaleLowerCase().includes(searchTerm) ||
        (location.city || '').toLocaleLowerCase().includes(searchTerm);

      const matchesCategory =
        selectedCategory === 'All' || location.category === selectedCategory;

      const matchesCity = selectedCity === 'All' || location.city === selectedCity;

      const matchesPrice =
        Number.isFinite(price) &&
        (minimum === null || price >= minimum) &&
        (maximum === null || price <= maximum);

      const matchesShootType =
        searchData.shootType === 'All' || searchData.shootType === '' || location.category === searchData.shootType;

      return matchesSearch && matchesCategory && matchesCity && matchesPrice && matchesShootType;
    });
  }, [locations, searchData, selectedCity, selectedCategory, minPrice, maxPrice]);

  const handleSearchChange = (event) => {
    const { name, value } = event.target;
    setSearchData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
  };

  const clearFilters = () => {
    setSearchData({ location: '', shootType: 'All', date: '' });
    setSelectedCity('All');
    setSelectedCategory('All');
    setMinPrice('');
    setMaxPrice('');
  };

  const hasInvalidPriceRange =
    minPrice !== '' && maxPrice !== '' && Number(minPrice) > Number(maxPrice);

  return (
    <div className="container section-block">
      <div className="section-heading">
        <p className="eyebrow">Explore</p>
        <h1>Explore Locations</h1>
      </div>

      {favoritesError && <p className="form-message form-message-error" role="alert">{favoritesError}</p>}

      <SearchBar
        searchData={searchData}
        onChange={handleSearchChange}
        onSubmit={handleSubmit}
        showShootType={false}
        showDate={false}
        className="explore-search-bar"
        locationLabel="Search locations"
        locationPlaceholder="Search by name or city"
      />

      <div className="filter-panel explore-filter-panel">
        <div className="filter-group">
          <label htmlFor="cityFilter">City</label>
          <select id="cityFilter" value={selectedCity} onChange={(e) => setSelectedCity(e.target.value)}>
            {cityOptions.map((city) => (
              <option key={city} value={city}>
                {city}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-group">
          <label htmlFor="categoryFilter">Category</label>
          <select id="categoryFilter" value={selectedCategory} onChange={(e) => setSelectedCategory(e.target.value)}>
            {categoryOptions.map((category) => (
              <option key={category} value={category}>
                {category}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-group">
          <label htmlFor="minPriceFilter">Minimum hourly price (₹)</label>
          <input
            id="minPriceFilter"
            type="number"
            min="0"
            step="100"
            value={minPrice}
            onChange={(event) => setMinPrice(event.target.value)}
            placeholder="Any"
          />
        </div>

        <div className="filter-group">
          <label htmlFor="maxPriceFilter">Maximum hourly price (₹)</label>
          <input
            id="maxPriceFilter"
            type="number"
            min="0"
            step="100"
            value={maxPrice}
            onChange={(event) => setMaxPrice(event.target.value)}
            placeholder="Any"
          />
        </div>

        <button type="button" className="btn btn-secondary small" onClick={clearFilters}>
          Clear filters
        </button>
      </div>

      {hasInvalidPriceRange && (
        <p className="form-message form-message-error" role="alert">
          Minimum hourly price cannot be greater than maximum hourly price.
        </p>
      )}

      <div className="results-summary">
        {!isLoading && !error && (
          <p>
            Showing <strong>{filteredLocations.length}</strong> locations
          </p>
        )}
      </div>

      <div className="location-grid">
        {isLoading ? (
          <div className="empty-state" role="status">
            <h3>Loading locations...</h3>
          </div>
        ) : error ? (
          <div className="empty-state" role="alert">
            <h3>Unable to load locations.</h3>
            <p>{error}</p>
          </div>
        ) : hasInvalidPriceRange ? (
          <div className="empty-state">
            <h3>Check your price range.</h3>
            <p>The minimum hourly price must not exceed the maximum.</p>
          </div>
        ) : filteredLocations.length > 0 ? (
          filteredLocations.map((location) => (
            <LocationCard
              key={location.id}
              location={location}
              isFavorite={favoriteIds.includes(String(location.id))}
              isFavoritePending={busyLocationId === String(location.id)}
              onToggleFavorite={toggleFavorite}
            />
          ))
        ) : locations.length === 0 ? (
          <div className="empty-state">
            <h3>No locations are available yet.</h3>
          </div>
        ) : (
          <div className="empty-state">
            <h3>No locations match your filters.</h3>
            <p>Try changing the search, city, category or price range.</p>
          </div>
        )}
      </div>
    </div>
  );
}

export default Explore;
