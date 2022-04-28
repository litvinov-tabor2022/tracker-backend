import sbt._

object Dependencies {

  val circeGenericExtras = "io.circe" %% "circe-generic-extras" % Versions.circe
  val circeLiteral = "io.circe" %% "circe-literal" % Versions.circe
  val circeParser = "io.circe" %% "circe-parser" % Versions.circe
  val kindProjector = "org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full
  val http4sCirce = "org.http4s" %% "http4s-circe" % "0.22.11"
  val log4cats = "org.typelevel" %% "log4cats-slf4j" % "1.5.1"
  val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.10"
  val mysql = "mysql" % "mysql-connector-java" % "8.0.28"
  val rabbitmq = "com.avast.clients.rabbitmq" %% "rabbitmq-client-core" % Versions.rabbitmq
  val rabbitmqPureconfig = "com.avast.clients.rabbitmq" %% "rabbitmq-client-pureconfig" % Versions.rabbitmq
  val rabbitmqCirce = "com.avast.clients.rabbitmq" %% "rabbitmq-client-extras-circe" % Versions.rabbitmq
  val semanticDb = "org.scalameta" % "semanticdb-scalac_2.13.8" % "4.5.0"
  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.9"
  val scalafixScaluzzi = "com.github.vovapolu" %% "scaluzzi" % "0.1.20"
  val scalafixSortImports = "com.nequissimus" %% "sort-imports" % "0.6.1"
  val sstBundleMonixHttp4sBlaze = "com.avast" %% "sst-bundle-monix-http4s-blaze" % Versions.sst
  val sstDoobieHikari = "com.avast" %% "sst-doobie-hikari" % Versions.sst
  val sstDoobieHikariPureConfig = "com.avast" %% "sst-doobie-hikari-pureconfig" % Versions.sst
  val sstFlywayPureConfig = "com.avast" %% "sst-flyway-pureconfig" % Versions.sst
  val sstJvm = "com.avast" %% "sst-jvm" % Versions.sst

  object Versions {
    val circe = "0.14.1"
    val doobie = "0.13.4"
    val rabbitmq = "9.0.0"
    val sst = "0.15.6"
    val silencer = "1.7.1"
  }

}
