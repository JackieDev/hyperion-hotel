DROP TABLE bookings;

CREATE TABLE IF NOT EXISTS bookings(
  room_id INT NOT NULL,
  customer_name VARCHAR NOT NULL,
  start_date TIMESTAMP WITH TIME ZONE NOT NULL,
  end_date TIMESTAMP WITH TIME ZONE NOT NULL,
  total_price DECIMAL,
  added_on TIMESTAMP WITH TIME ZONE DEFAULT now(),
  PRIMARY KEY (room_id, start_date)
);