package com.hyperion.hotel.database

import scala.util.control.NoStackTrace

case class DatabaseError(message: String) extends NoStackTrace
