import assert from 'node:assert/strict';
import test from 'node:test';
import {
  addHoursToTime,
  formatTime,
  overlapsTimeInterval,
  parseTimeTo12Hour,
  timeToMinutes,
  to24HourTime,
} from './time.js';

test('formats afternoon times with PM', () => {
  assert.equal(formatTime('14:00'), '2:00 PM');
});

test('formats midnight and noon correctly', () => {
  assert.equal(formatTime('00:00'), '12:00 AM');
  assert.equal(formatTime('12:00'), '12:00 PM');
});

test('supports LocalTime values with seconds', () => {
  assert.equal(formatTime('09:05:00'), '9:05 AM');
});

test('preserves invalid or already formatted values', () => {
  assert.equal(formatTime('25:00'), '25:00');
  assert.equal(formatTime('2:00 PM'), '2:00 PM');
});

test('converts 12 AM and 12 PM selectors into backend time values', () => {
  assert.equal(to24HourTime('12', '00', 'AM'), '00:00');
  assert.equal(to24HourTime('12', '00', 'PM'), '12:00');
  assert.equal(to24HourTime('1', '30', 'PM'), '13:30');
  assert.equal(to24HourTime('', '30', 'PM'), '');
  assert.equal(to24HourTime('1', '', 'PM'), '');
  assert.equal(to24HourTime('1', '30', ''), '');
});

test('parses backend time values into selectable 12-hour parts', () => {
  assert.deepEqual(parseTimeTo12Hour('00:00'), { hour: '12', minute: '00', period: 'AM' });
  assert.deepEqual(parseTimeTo12Hour('12:00'), { hour: '12', minute: '00', period: 'PM' });
  assert.deepEqual(parseTimeTo12Hour('18:00'), { hour: '6', minute: '00', period: 'PM' });
  assert.deepEqual(parseTimeTo12Hour(''), { hour: '', minute: '', period: '' });
});

test('calculates end times and detects crossing midnight', () => {
  assert.equal(addHoursToTime('13:30', 2), '15:30');
  assert.equal(addHoursToTime('22:00', 2), '');
});

test('converts API times to minutes for conflict comparisons', () => {
  assert.equal(timeToMinutes('13:30:00'), 810);
  assert.equal(timeToMinutes('25:00'), null);
});

test('reports only actual interval overlaps, allowing adjacent slots', () => {
  assert.equal(overlapsTimeInterval('10:00', '12:00', '10:00', '12:00'), true);
  assert.equal(overlapsTimeInterval('12:00', '14:00', '10:00', '12:00'), false);
  assert.equal(overlapsTimeInterval('09:00', '10:00', '10:00', '12:00'), false);
  assert.equal(overlapsTimeInterval('10:00', '12:00', 'bad', '12:00'), false);
});
