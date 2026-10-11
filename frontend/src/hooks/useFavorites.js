import { useCallback, useEffect, useState } from 'react';
import { getAuthToken } from '../services/auth';
import {
  addUserFavorite,
  fetchUserFavorites,
  removeUserFavorite,
} from '../services/favorites';

function useFavorites() {
  const [favorites, setFavorites] = useState([]);
  const [isLoading, setIsLoading] = useState(Boolean(getAuthToken()));
  const [error, setError] = useState('');
  const [busyLocationId, setBusyLocationId] = useState(null);

  useEffect(() => {
    if (!getAuthToken()) {
      return undefined;
    }

    const controller = new AbortController();
    fetchUserFavorites(controller.signal)
      .then((loadedFavorites) => {
        if (!controller.signal.aborted) setFavorites(loadedFavorites);
      })
      .catch((requestError) => {
        if (!controller.signal.aborted) {
          setError(requestError.message || 'Unable to load your favourites.');
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false);
      });
    return () => controller.abort();
  }, []);

  const toggleFavorite = useCallback(async (location) => {
    const locationId = String(location.id);
    setBusyLocationId(locationId);
    setError('');
    try {
      if (favorites.some((item) => String(item.id) === locationId)) {
        await removeUserFavorite(locationId);
        setFavorites((current) => current.filter((item) => String(item.id) !== locationId));
      } else {
        await addUserFavorite(locationId);
        setFavorites((current) => (
          current.some((item) => String(item.id) === locationId)
            ? current
            : [...current, location]
        ));
      }
    } catch (requestError) {
      setError(requestError.message || 'Unable to update your favourites.');
    } finally {
      setBusyLocationId(null);
    }
  }, [favorites]);

  const favoriteIds = favorites.map((location) => String(location.id));

  return {
    favorites,
    favoriteIds,
    isLoading,
    error,
    busyLocationId,
    toggleFavorite,
  };
}

export default useFavorites;
