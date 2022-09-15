CREATE TABLE IF NOT EXISTS bookings(
  room_id NUM NOT NULL,
  customer_name VARCHAR NOT NULL,
  start_date TIMESTAMP WITH TIME ZONE,
  end_date TIMESTAMP WITH TIME ZONE,
  added_on TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX IF NOT EXISTS all_bookings_idx ON bookings(room_id, start_date, added_on);