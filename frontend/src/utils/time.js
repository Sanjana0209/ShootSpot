export function formatTime(value) {
  if (typeof value !== 'string') return '';

  const match = /^(\d{2}):(\d{2})(?::\d{2}(?:\.\d+)?)?$/.exec(value);
  if (!match) return value;

  const hours = Number(match[1]);
  const minutes = Number(match[2]);
  if (hours > 23 || minutes > 59) return value;

  const period = hours >= 12 ? 'PM' : 'AM';
  const displayHours = hours % 12 || 12;
  return `${displayHours}:${match[2]} ${period}`;
}

export function parseTimeTo12Hour(value) {
  const match = /^(\d{2}):(\d{2})$/.exec(value || '');
  if (!match) return { hour: '', minute: '', period: '' };

  const hours = Number(match[1]);
  const minutes = Number(match[2]);
  if (hours > 23 || minutes > 59) return { hour: '', minute: '', period: '' };

  return {
    hour: String(hours % 12 || 12),
    minute: match[2],
    period: hours >= 12 ? 'PM' : 'AM',
  };
}

export function to24HourTime(hour, minute, period) {
  if (hour === '' || minute === '') return '';
  const displayHour = Number(hour);
  const displayMinute = Number(minute);
  if (
    !Number.isInteger(displayHour) || displayHour < 1 || displayHour > 12
    || !Number.isInteger(displayMinute) || displayMinute < 0 || displayMinute > 59
    || !['AM', 'PM'].includes(period)
  ) {
    return '';
  }

  const hours = (displayHour % 12) + (period === 'PM' ? 12 : 0);
  return `${String(hours).padStart(2, '0')}:${String(displayMinute).padStart(2, '0')}`;
}

export function addHoursToTime(value, durationHours) {
  const minutesSinceMidnight = timeToMinutes(value);
  const duration = Number(durationHours);
  if (minutesSinceMidnight == null || !Number.isInteger(duration) || duration < 1) return '';

  const endMinutes = minutesSinceMidnight + duration * 60;
  if (endMinutes >= 24 * 60) return '';
  return `${String(Math.floor(endMinutes / 60)).padStart(2, '0')}:${String(endMinutes % 60).padStart(2, '0')}`;
}

export function timeToMinutes(value) {
  const match = /^(\d{2}):(\d{2})(?::\d{2}(?:\.\d+)?)?$/.exec(value || '');
  if (!match) return null;

  const hours = Number(match[1]);
  const minutes = Number(match[2]);
  if (hours > 23 || minutes > 59) return null;
  return hours * 60 + minutes;
}

export function overlapsTimeInterval(requestStart, requestEnd, intervalStart, intervalEnd) {
  const requestedStartMinutes = timeToMinutes(requestStart);
  const requestedEndMinutes = timeToMinutes(requestEnd);
  const occupiedStartMinutes = timeToMinutes(intervalStart);
  const occupiedEndMinutes = timeToMinutes(intervalEnd);

  return requestedStartMinutes != null
    && requestedEndMinutes != null
    && occupiedStartMinutes != null
    && occupiedEndMinutes != null
    && occupiedStartMinutes < requestedEndMinutes
    && occupiedEndMinutes > requestedStartMinutes;
}
