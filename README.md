# hyperion-hotel

![hotel](hyperion.jpg)

This microservice is the backend of a hotel reservation application created 
using Scala, Cats, Circe, Doobie, Postgres and Http4s.

### Endpoints
GET bookings/roomId

GET bookings-for/customerName

POST new-booking (supply booking details)

POST cancel-booking (supply booking details)

POST available-rooms (supply dates)

GET remove-expired (bookings)
