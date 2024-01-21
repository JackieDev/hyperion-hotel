# hyperion-hotel

![hotel](hyperion.jpg)

This microservice is the backend of a hotel reservation application created 
using Scala, Cats, Circe, Doobie, Postgres and Http4s.

### Endpoints
GET bookings/roomId

GET bookings-for/customerName

GET remove-expired (bookings)

POST new-booking (supply booking details in the body)
```
{
    "roomId":"411",
    "customerName":"AJ",
    "startDate":"2024-02-11T15:00:00Z",
    "endDate":"2024-02-15T12:00:00Z"
}
```

POST cancel-booking (supply booking details in the body)
```
{
    "roomId":"411",
    "customerName":"AJ",
    "startDate":"2024-02-11T15:00:00Z",
    "endDate":"2024-02-15T12:00:00Z"
}
```

POST available-rooms (supply dates in the body)
```
{
    "startDate":"2024-02-11T15:00:00Z",
    "endDate":"2024-02-15T12:00:00Z",
}
```

POST special-deal-booking / specialId (supply booking details in the body)
```
{
    "roomId":"411",
    "customerName":"AJ",
    "startDate":"2024-02-11T15:00:00Z",
    "endDate":"2024-02-15T12:00:00Z"
}
```

Current special deal Ids available:
```
VALENTINES - for valentines offer
MAYBH - for May Bank Holiday offer
JJ - for Jolly July offer
DEC - for all of December offer
```
