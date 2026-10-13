import { useState } from 'react';
import { Link } from 'react-router-dom';

const FALLBACK_IMAGE_URL = 'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?auto=format&fit=crop&w=1200&q=80';
const PHOTO_CREDITS = new Map([
  [
    'https://thumb.wikimedia.org/wikipedia/commons/thumb/7/71/Charminar_Hyderabad_1.jpg/1280px-Charminar_Hyderabad_1.jpg',
    {
      author: 'DidierTais',
      sourceUrl: 'https://commons.wikimedia.org/wiki/File:Charminar_Hyderabad_1.jpg',
      license: 'CC BY-SA 3.0',
      licenseUrl: 'https://creativecommons.org/licenses/by-sa/3.0/',
    },
  ],
  [
    'https://commons.wikimedia.org/wiki/Special:FilePath/Dancing_Girls_Bath_%28Hampi_water_tank%29%2C_Hampi%2C_Vijayanagara%2C_Karnataka%2C_India_%282012%29.jpg?width=1280',
    {
      author: 'Dey.sandip',
      sourceUrl: 'https://commons.wikimedia.org/wiki/File:Dancing_Girls_Bath_(Hampi_water_tank),_Hampi,_Vijayanagara,_Karnataka,_India_(2012).jpg',
      license: 'CC BY-SA 3.0',
      licenseUrl: 'https://creativecommons.org/licenses/by-sa/3.0/',
    },
  ],
  [
    'https://thumb.wikimedia.org/wikipedia/commons/thumb/7/7d/Band-Stand-Cubbon-Park-Bengaluru-karnataka-India.jpg/1280px-Band-Stand-Cubbon-Park-Bengaluru-karnataka-India.jpg',
    {
      author: 'N. Vivekananthamoorthy',
      sourceUrl: 'https://commons.wikimedia.org/wiki/File:Band-Stand-Cubbon-Park-Bengaluru-karnataka-India.jpg',
      license: 'CC BY 4.0',
      licenseUrl: 'https://creativecommons.org/licenses/by/4.0/',
    },
  ],
  [
    'https://thumb.wikimedia.org/wikipedia/commons/thumb/c/cf/Anjuna_Beach%2C_Goa%2C_India%2C_Legendary_Curlies_beach_shack.jpg/1280px-Anjuna_Beach%2C_Goa%2C_India%2C_Legendary_Curlies_beach_shack.jpg',
    {
      author: 'Vyacheslav Argenberg',
      sourceUrl: 'https://commons.wikimedia.org/wiki/File:Anjuna_Beach,_Goa,_India,_Legendary_Curlies_beach_shack.jpg',
      license: 'CC BY 4.0',
      licenseUrl: 'https://creativecommons.org/licenses/by/4.0/',
    },
  ],
  [
    'https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b3/Mandrem_Beach_and_Mandrem_River%2C_Mandrem%2C_Goa%2C_India.jpg/1280px-Mandrem_Beach_and_Mandrem_River%2C_Mandrem%2C_Goa%2C_India.jpg',
    {
      author: 'Vyacheslav Argenberg',
      sourceUrl: 'https://commons.wikimedia.org/wiki/File:Mandrem_Beach_and_Mandrem_River,_Mandrem,_Goa,_India.jpg',
      license: 'CC BY 4.0',
      licenseUrl: 'https://creativecommons.org/licenses/by/4.0/',
    },
  ],
  [
    'https://upload.wikimedia.org/wikipedia/commons/0/0a/House_Boat_at_night%2C_Alappuzha%2C_Kerala.jpg',
    {
      author: 'Devender Goyal',
      sourceUrl: 'https://commons.wikimedia.org/wiki/File:House_Boat_at_night,_Alappuzha,_Kerala.jpg',
      license: 'CC BY-SA 4.0',
      licenseUrl: 'https://creativecommons.org/licenses/by-sa/4.0/',
    },
  ],
  [
    'https://upload.wikimedia.org/wikipedia/commons/5/59/Kerala_house_boats.jpg',
    {
      author: 'Lijovklm',
      sourceUrl: 'https://commons.wikimedia.org/wiki/File:Kerala_house_boats.jpg',
      license: 'CC BY-SA 4.0',
      licenseUrl: 'https://creativecommons.org/licenses/by-sa/4.0/',
    },
  ],
  [
    'https://thumb.wikimedia.org/wikipedia/commons/thumb/7/7a/Jaipur%2C_India%2C_Hawa_Mahal_Palace.jpg/1280px-Jaipur%2C_India%2C_Hawa_Mahal_Palace.jpg',
    {
      author: 'Vyacheslav Argenberg',
      sourceUrl: 'https://commons.wikimedia.org/wiki/File:Jaipur,_India,_Hawa_Mahal_Palace.jpg',
      license: 'CC BY 4.0',
      licenseUrl: 'https://creativecommons.org/licenses/by/4.0/',
    },
  ],
]);

function LocationImage({ src, alt, details = false, linkTo, showCredit = false }) {
  const [usingFallback, setUsingFallback] = useState(!src);
  const [fallbackFailed, setFallbackFailed] = useState(false);
  const className = `location-image${details ? ' location-image-details' : ''}`;
  const photoCredit = src
    ? PHOTO_CREDITS.get(src) || PHOTO_CREDITS.get(src.split('?')[0])
    : null;

  const handleError = () => {
    if (usingFallback) {
      setFallbackFailed(true);
    } else {
      setUsingFallback(true);
    }
  };

  return (
    <>
      <div className={className}>
        {fallbackFailed ? (
          <div className="location-image-placeholder" role="img" aria-label={`${alt}: photo unavailable`}>
            Photo unavailable
          </div>
        ) : (
          linkTo ? (
            <Link to={linkTo} aria-label={`View details for ${alt}`}>
              <img src={usingFallback ? FALLBACK_IMAGE_URL : src} alt={alt} onError={handleError} />
            </Link>
          ) : (
            <img src={usingFallback ? FALLBACK_IMAGE_URL : src} alt={alt} onError={handleError} />
          )
        )}
      </div>
      {showCredit && !usingFallback && !fallbackFailed && photoCredit && (
        <div className="photo-credit" aria-label="Photo attribution">
          <a href={photoCredit.sourceUrl} target="_blank" rel="noopener noreferrer">
            Photo: {photoCredit.author}
          </a>
          {' · '}
          <a href={photoCredit.licenseUrl} target="_blank" rel="noopener noreferrer">
            {photoCredit.license}
          </a>
        </div>
      )}
    </>
  );
}

export default LocationImage;
