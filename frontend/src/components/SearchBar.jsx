function SearchBar({
  searchData,
  onChange,
  onSubmit,
  showShootType = true,
  showDate = true,
  className = '',
  locationLabel = 'Location',
  locationPlaceholder = 'City or area',
}) {
  return (
    <form className={`search-bar ${className}`.trim()} onSubmit={onSubmit}>
      <div className="search-field">
        <label htmlFor="location">{locationLabel}</label>
        <input
          id="location"
          name="location"
          type="text"
          value={searchData.location}
          onChange={onChange}
          placeholder={locationPlaceholder}
        />
      </div>

      {showShootType && (
        <div className="search-field">
          <label htmlFor="shootType">Shoot Type</label>
          <select id="shootType" name="shootType" value={searchData.shootType} onChange={onChange}>
            <option value="">All</option>
            <option value="Photoshoot">Photoshoot</option>
            <option value="Reels">Reels</option>
            <option value="Short Film">Short Film</option>
            <option value="Advertisement">Advertisement</option>
            <option value="Pre-wedding">Pre-wedding</option>
          </select>
        </div>
      )}

      {showDate && (
        <div className="search-field">
          <label htmlFor="date">Date</label>
          <input id="date" name="date" type="date" value={searchData.date} onChange={onChange} />
        </div>
      )}

      <button type="submit" className="btn btn-primary search-button">
        Search
      </button>
    </form>
  );
}

export default SearchBar;
