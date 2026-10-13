import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import CategoryCard from '../components/CategoryCard';
import LocationCard from '../components/LocationCard';
import SearchBar from '../components/SearchBar';
import useFavorites from '../hooks/useFavorites';
import { fetchLocations } from '../services/locations';

const categoryList = [
  { title: 'Photoshoots', description: 'Professional backdrops for portrait and fashion stories.', icon: '📷' },
  { title: 'Reels', description: 'Trendy spaces built for short-form motion storytelling.', icon: '🎥' },
  { title: 'Short Films', description: 'Cinematic environments for elevated, story-driven scenes.', icon: '🎬' },
  { title: 'Advertisements', description: 'Premium commercial spaces with clean, branded visuals.', icon: '📢' },
  { title: 'Pre-wedding', description: 'Romantic settings designed for unforgettable memories.', icon: '💍' },
];

const reasons = [
  'Verified locations',
  'Easy booking',
  'Transparent pricing',
  'Multiple shoot categories',
];

function Home() {
  const [featuredLocations, setFeaturedLocations] = useState([]);
  const [isLoadingLocations, setIsLoadingLocations] = useState(true);
  const [locationsError, setLocationsError] = useState('');
  const [searchData, setSearchData] = useState({
    location: '',
    shootType: '',
    date: '',
  });
  const { favoriteIds, error: favoritesError, busyLocationId, toggleFavorite } = useFavorites();

  useEffect(() => {
    const controller = new AbortController();

    fetchLocations(controller.signal)
      .then((locations) => setFeaturedLocations(locations.slice(0, 4)))
      .catch((error) => {
        if (error.name !== 'AbortError') {
          setLocationsError(error.message || 'Unable to load featured locations.');
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setIsLoadingLocations(false);
        }
      });

    return () => controller.abort();
  }, []);

  const handleSearchChange = (event) => {
    const { name, value } = event.target;
    setSearchData((prev) => ({ ...prev, [name]: value }));
  };

  return (
    <>
      <section className="hero-section container">
        <div className="hero-copy">
          <p className="eyebrow">Professional location booking</p>
          <h1>Find the Perfect Location for Your Shoot</h1>
          <p className="lead">
            Discover unique locations for photoshoots, reels, short films, advertisements and
            pre-wedding shoots.
          </p>

          <div className="hero-actions">
            <Link to="/explore" className="btn btn-primary">
              Explore Locations
            </Link>
            <Link to="/admin/locations" className="btn btn-secondary">
              List Your Location
            </Link>
          </div>
        </div>

        <div className="hero-visual" aria-label="Location showcase">
          <div className="visual-card hero-card main">
            <img
              src="https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=900&q=80"
              alt="Modern photo studio"
            />
          </div>
          <div className="visual-card hero-card badge-card">
            <span className="mini-label">Popular</span>
            <strong>Skyline Terrace</strong>
            <p>Hyderabad • ₹1,800/hr</p>
          </div>
        </div>
      </section>

      <section className="container section-block">
        <div className="section-heading centered">
          <p className="eyebrow">Search</p>
          <h2>Plan your next shoot with confidence</h2>
        </div>
        <SearchBar searchData={searchData} onChange={handleSearchChange} onSubmit={(e) => e.preventDefault()} />
      </section>

      <section className="container section-block" id="categories">
        <div className="section-heading">
          <p className="eyebrow">Categories</p>
          <h2>Popular Categories</h2>
        </div>
        <div className="category-grid">
          {categoryList.map((item) => (
            <CategoryCard key={item.title} title={item.title} description={item.description} icon={item.icon} />
          ))}
        </div>
      </section>

      <section className="container section-block">
        <div className="section-heading row-heading">
          <div>
            <p className="eyebrow">Locations</p>
            <h2>Featured Locations</h2>
          </div>
          <Link to="/explore" className="text-link">
            View all
          </Link>
        </div>

        <div className="location-grid">
          {favoritesError && (
            <p className="form-message form-message-error" role="alert">{favoritesError}</p>
          )}
          {isLoadingLocations ? (
            <div className="empty-state" role="status">
              <h3>Loading featured locations...</h3>
            </div>
          ) : locationsError ? (
            <div className="empty-state" role="alert">
              <h3>Unable to load featured locations.</h3>
              <p>{locationsError}</p>
            </div>
          ) : featuredLocations.length === 0 ? (
            <div className="empty-state">
              <h3>No locations are available yet.</h3>
              <p>Check back when locations have been added.</p>
            </div>
          ) : featuredLocations.map((location) => (
            <LocationCard
              key={location.id}
              location={location}
              isFavorite={favoriteIds.includes(String(location.id))}
              isFavoritePending={busyLocationId === String(location.id)}
              onToggleFavorite={toggleFavorite}
            />
          ))}
        </div>
      </section>

      <section className="container section-block" id="how-it-works">
        <div className="section-heading centered">
          <p className="eyebrow">Simple process</p>
          <h2>How It Works</h2>
        </div>

        <div className="steps-grid">
          <div className="step-card">
            <span className="step-number">1</span>
            <h3>Discover</h3>
            <p>Browse curated locations that match your creative vision and budget.</p>
          </div>
          <div className="step-card">
            <span className="step-number">2</span>
            <h3>Choose</h3>
            <p>Compare amenities, pricing and ratings to pick the right setup.</p>
          </div>
          <div className="step-card">
            <span className="step-number">3</span>
            <h3>Book</h3>
            <p>Reserve your ideal space quickly and move forward with your shoot schedule.</p>
          </div>
        </div>
      </section>

      <section className="container section-block" id="about">
        <div className="section-heading centered">
          <p className="eyebrow">Why choose us</p>
          <h2>Why Choose ShootSpot</h2>
        </div>
        <div className="features-grid">
          {reasons.map((reason) => (
            <div key={reason} className="feature-item">
              <span className="feature-check">✓</span>
              <span>{reason}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="container cta-section">
        <div>
          <h2>Ready to find your perfect shoot location?</h2>
        </div>
        <Link to="/explore" className="btn btn-primary">
          Explore Locations
        </Link>
      </section>
    </>
  );
}

export default Home;
