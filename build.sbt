name := "hyperion-hotel"

version := "0.1"

scalaVersion := "2.13.8"

resolvers += "confluent" at "https://packages.confluent.io/maven/"

val cats         = "2.4.2"
val circe        = "0.13.0"
val doobie       = "0.12.1"
val flyway       = "7.7.2"
val fs2Kafka     = "1.11.0"
val http4s       = "0.21.20"
val postgresql   = "42.2.6"
val pureConfig   = "0.14.1"
val refined      = "0.9.13"
val vulcan       = "1.6.0"

libraryDependencies ++= Seq(
  "org.typelevel"          %% "cats-core"            % cats,
  "io.circe"               %% "circe-core"           % circe,
  "io.circe"               %% "circe-generic"        % circe,
  "io.circe"               %% "circe-refined"        % circe,
  "io.circe"               %% "circe-parser"         % circe,
  "org.tpolecat"           %% "doobie-core"          % doobie,
  "org.tpolecat"           %% "doobie-hikari"        % doobie,
  "org.tpolecat"           %% "doobie-postgres"      % doobie,
  "org.tpolecat"           %% "doobie-refined"       % doobie,
  "org.flywaydb"           %  "flyway-core"          % flyway,
  "com.github.fd4s"        %% "fs2-kafka"            % fs2Kafka,
  "com.github.fd4s"        %% "fs2-kafka-vulcan"     % vulcan,
  "com.github.fd4s"        %% "vulcan"               % vulcan,
  "org.http4s"             %% "http4s-blaze-server"  % http4s,
  "org.http4s"             %% "http4s-blaze-client"  % http4s,
  "org.http4s"             %% "http4s-circe"         % http4s,
  "org.http4s"             %% "http4s-dsl"           % http4s,
  "org.postgresql"         %  "postgresql"           % postgresql,
  "com.github.pureconfig"  %% "pureconfig"           % pureConfig,
  "com.typesafe.scala-logging" %% "scala-logging"    % "3.9.3",
  "com.lihaoyi"            %% "sourcecode"           % "0.2.5",
  "org.scalatest"          %% "scalatest"            % "3.2.17" % "test"
)


