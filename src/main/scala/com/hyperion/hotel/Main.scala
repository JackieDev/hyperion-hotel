package com.hyperion.hotel

import java.util.concurrent.Executors
import cats.effect.{Blocker, ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, LiftIO, Resource, Timer}
import cats.syntax.all._
import com.hyperion.hotel.config.ServiceConfig
import com.hyperion.hotel.database.{PostgresStore, SchemaMigration, Store}
import com.hyperion.hotel.handlers.BookingHandler
import com.hyperion.hotel.models.{Hotel, Room, Suite}
import com.hyperion.hotel.routing.Routes
import com.typesafe.scalalogging.Logger
import doobie.free.connection.ConnectionIO
import fs2.Stream
import org.http4s.HttpRoutes
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  implicit val concurrentEffect: ConcurrentEffect[IO] = IO.ioConcurrentEffect

  val logger: Logger = Logger(getClass)

  val angelsRoom: Room = Room(217, true, Suite)
  val theHotel: Hotel = Hotel("Hyperion Hotel, Los Angeles", 4, 17, List(angelsRoom))
  val totalRooms: Int = theHotel.roomsPerFloor * theHotel.floors

  val firstFloor: List[Room] = (101 to 117).map(Room.createStandardRoom).toList
  val secondFloor: List[Room] = (201 to 217).map(Room.createDeluxeRoom).toList
  val thirdFloor: List[Room] = (301 to 317).map(Room.createStandardRoom).toList
  val fourthFloor: List[Room] = (401 to 417).map(Room.createSuiteRoom).toList

  def generateRooms: List[Room] = {
//    def go(roomsSoFar: List[Room], currentRoomNumber: Int): List[Room] = {
//      if (currentRoomNumber < totalRooms && (roomsSoFar.exists(_.id != currentRoomNumber)))
//        go(roomsSoFar ++ List(Room(currentRoomNumber, false)), currentRoomNumber+1)
//      else if (currentRoomNumber == totalRooms && (roomsSoFar.exists(_.id != currentRoomNumber)))
//        roomsSoFar ++ List(Room(currentRoomNumber, false))
//      else if (currentRoomNumber < totalRooms || currentRoomNumber == totalRooms)
//        go(roomsSoFar, currentRoomNumber+1)
//      else
//        roomsSoFar
//    }

    //go(theHotel.offLimitRooms, 1)

    (firstFloor ++
      secondFloor ++
      thirdFloor ++
      fourthFloor).filterNot(room => (room.id == angelsRoom.id))
  }

  def databaseResource(config: ServiceConfig): Resource[IO, Store[IO, ConnectionIO]] =
    for {
      _ <- Resource.eval(logger.info("-------------- Loading hyperion-hotel database...").pure[IO])
      _ <- Resource.eval(SchemaMigration[IO](config.hyperionHotel.db))
      block <- Blocker[IO]
      dbES = Executors.newFixedThreadPool(config.hyperionHotel.db.maxConnectionPoolSize)
      dbEC = ExecutionContext.fromExecutorService(dbES)
      db <- PostgresStore.resource(config.hyperionHotel.db, dbEC, block, LiftIO.liftK[ConnectionIO])
    } yield db

  private def runServer(service: HttpRoutes[IO], host: String, port: Int, ec: ExecutionContext)(
    implicit timer: Timer[IO],
    cs: ContextShift[IO]
  ): Stream[IO, ExitCode] =
    BlazeServerBuilder[IO](ec)
      .bindHttp(port, host)
      .withHttpApp(service.orNotFound)
      .serve


  def stream: Stream[IO, ExitCode] =
    for {
      configs <- Stream.eval(ServiceConfig.loadApplicationConfig.onError {
        case err => logger.error(s"Hyperion hotel service is unable to load config, aborting. Failed with error: $err").pure[IO]
      })

      (config, rawConfig) = configs

      appExecutorService = Executors.newFixedThreadPool(8)
      appExecutionContext = ExecutionContext.fromExecutorService(appExecutorService)
      db <- Stream.resource(databaseResource(config))
      generatedRooms = generateRooms
      bookingHandler = new BookingHandler[IO, ConnectionIO](db, generatedRooms)
      httpService = new Routes[IO, ConnectionIO](db, bookingHandler).routes
      server <- runServer(httpService, config.hyperionHotel.httpd.host, config.hyperionHotel.httpd.port, appExecutionContext)

    } yield server


  override def run(args: List[String]): IO[ExitCode] = {

    stream.compile.drain.as(ExitCode.Success)
  }

}
