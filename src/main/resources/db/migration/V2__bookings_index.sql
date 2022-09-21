DROP INDEX all_bookings_idx;
CREATE INDEX IF NOT EXISTS bookings_idx ON bookings(room_id, start_date);